package com.lumie.exam.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.test.TenantContextExtension;
import com.lumie.exam.application.dto.request.OmrGradingCallbackRequest;
import com.lumie.exam.application.port.out.CallbackDeduplicationPort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.exam.domain.vo.JobStatus;
import com.lumie.exam.test.ExamFixtures;
import com.lumie.student.api.StudentService;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, TenantContextExtension.class})
@DisplayName("OmrGradingJobProcessor 단위 테스트")
class OmrGradingJobProcessorTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamResultRepository resultRepository;
    @Mock
    private ResultCommandService resultCommandService;
    @Mock
    private OmrStoragePort storagePort;
    @Mock
    private CallbackDeduplicationPort callbackDeduplication;
    @Mock
    private StudentService studentService;
    @Mock
    private OmrGradingJobRepository jobRepository;

    private OmrGradingJobProcessor processor;

    @BeforeEach
    void setUp() {
        OmrGradingJobProcessor.Tx tx = new OmrGradingJobProcessor.Tx(
                examRepository, resultRepository, resultCommandService, jobRepository, studentService);
        processor = new OmrGradingJobProcessor(storagePort, callbackDeduplication, jobRepository, tx);
    }

    private OmrGradingCallbackRequest buildSuccessCallback() {
        return new OmrGradingCallbackRequest(
                1L, 10L, "inst-abc123",
                "omr/tmp/abc123/batch1/sheet1.png",
                0, 1,
                true, null,
                "12345678", 100, 1,
                List.of(
                        new OmrGradingCallbackRequest.QuestionResultItem(1, "1", "1", 30, 30, "SHORT"),
                        new OmrGradingCallbackRequest.QuestionResultItem(2, "2", "2", 30, 30, "SHORT"),
                        new OmrGradingCallbackRequest.QuestionResultItem(3, "3", "3", 40, 40, "SHORT")
                )
        );
    }

    @Test
    @DisplayName("콜백 성공 시 결과를 저장하고 incrementSuccess를 호출한다")
    void processGradingCallback_success() {
        Exam exam = ExamFixtures.aGradedExam(3);
        OmrGradingCallbackRequest request = buildSuccessCallback();
        Long studentId = 200L;
        StudentService.StudentData student = new StudentService.StudentData(
                studentId, 1000L, "홍길동", "01012345678", true, null, null);
        ExamResult resolvedResult = ExamResult.create(exam, studentId, 100);

        given(callbackDeduplication.markIfAbsent(any())).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(eq(1L), eq(List.of(JobStatus.PENDING, JobStatus.PROCESSING))))
                .willReturn(true);
        given(examRepository.findById(10L)).willReturn(Optional.of(exam));
        given(studentService.getStudentByPhone(any(), eq("01012345678"))).willReturn(Optional.of(student));
        given(resultCommandService.resolveResult(eq(exam), eq(studentId), any())).willReturn(resolvedResult);
        given(storagePort.getObject(any())).willReturn(new byte[]{1, 2, 3});
        given(storagePort.putObject(any(), any(), any())).willReturn("omr/results/abc123/10/200");
        given(resultRepository.saveAndFlush(any(ExamResult.class))).willReturn(resolvedResult);
        given(jobRepository.tryFinalize(1L, 1)).willReturn(true);

        processor.processGradingCallback(request);

        verify(jobRepository).incrementSuccess(1L);
        verify(resultRepository).saveAndFlush(any(ExamResult.class));
        // 가입 학생도 채점 결과에 전화번호가 저장되어야 한다 (010 + 콜백 phoneNumber).
        assertThat(resolvedResult.getPhoneNumber()).isEqualTo("01012345678");
    }

    @Test
    @DisplayName("등록 학생 결과 저장이 unique 충돌하면 기존 결과를 다시 찾아 덮어쓴다")
    void processGradingCallback_registeredDuplicate_overwritesExistingResult() {
        OmrGradingCallbackRequest request = buildSuccessCallback();
        Exam exam = ExamFixtures.aGradedExam(3);
        StudentService.StudentData student = new StudentService.StudentData(
                200L, 1000L, "홍길동", "01012345678", true, null, null);
        ExamResult resolvedResult = ExamResult.create(exam, student.id(), 100);
        OmrGradingJobProcessor.Tx retryTx = mock(OmrGradingJobProcessor.Tx.class);
        OmrGradingJobProcessor retryingProcessor =
                new OmrGradingJobProcessor(storagePort, callbackDeduplication, jobRepository, retryTx);
        OmrGradingJobProcessor.Tx.SuccessPrep prep =
                new OmrGradingJobProcessor.Tx.SuccessPrep(
                        exam,
                        Optional.of(student),
                        resolvedResult,
                        "inst-abc123/omr/10/200"
                );

        given(callbackDeduplication.markIfAbsent("omr:cb:1:0")).willReturn(true);
        given(retryTx.isOpenJob(1L)).willReturn(true);
        given(retryTx.prepareSuccess(request)).willReturn(prep);
        given(storagePort.getObject(any())).willReturn(new byte[]{1, 2, 3});
        given(storagePort.putObject(any(), any(), any())).willReturn("inst-abc123/omr/10/200");
        doThrow(new DataIntegrityViolationException("uq_exam_result_student"))
                .when(retryTx)
                .saveSuccess(eq(request), any(OmrGradingJobProcessor.Tx.SuccessPrep.class));
        given(retryTx.finalizeIfDone(1L, 1)).willReturn(true);

        retryingProcessor.processGradingCallback(request);

        verify(retryTx).saveSuccess(eq(request), any(OmrGradingJobProcessor.Tx.SuccessPrep.class));
        verify(retryTx).overwriteRegisteredResult(request, student.id(), "01012345678", "inst-abc123/omr/10/200");
        verify(retryTx).finalizeIfDone(1L, 1);
        verify(callbackDeduplication, never()).forget("omr:cb:1:0");
    }

    @Test
    @DisplayName("optimistic lock 실패는 dedup을 해제한 뒤 재시도한다")
    void processGradingCallback_optimisticLockFailure_releasesDedupAndRetries() {
        OmrGradingCallbackRequest request = buildSuccessCallback();
        Exam exam = ExamFixtures.aGradedExam(3);
        StudentService.StudentData student = new StudentService.StudentData(
                200L, 1000L, "홍길동", "01012345678", true, null, null);
        ExamResult resolvedResult = ExamResult.create(exam, student.id(), 100);
        OmrGradingJobProcessor.Tx retryTx = mock(OmrGradingJobProcessor.Tx.class);
        OmrGradingJobProcessor retryingProcessor =
                new OmrGradingJobProcessor(storagePort, callbackDeduplication, jobRepository, retryTx);
        OmrGradingJobProcessor.Tx.SuccessPrep prep =
                new OmrGradingJobProcessor.Tx.SuccessPrep(
                        exam,
                        Optional.of(student),
                        resolvedResult,
                        "inst-abc123/omr/10/200"
                );

        given(callbackDeduplication.markIfAbsent("omr:cb:1:0")).willReturn(true, true);
        given(retryTx.isOpenJob(1L)).willReturn(true);
        given(retryTx.prepareSuccess(request)).willReturn(prep);
        given(storagePort.getObject(any())).willReturn(new byte[]{1, 2, 3});
        given(storagePort.putObject(any(), any(), any())).willReturn("inst-abc123/omr/10/200");
        doThrow(new OptimisticLockingFailureException("stale result"))
                .doNothing()
                .when(retryTx)
                .saveSuccess(eq(request), any(OmrGradingJobProcessor.Tx.SuccessPrep.class));
        given(retryTx.finalizeIfDone(1L, 1)).willReturn(true);

        retryingProcessor.processGradingCallback(request);

        verify(callbackDeduplication).forget("omr:cb:1:0");
        verify(retryTx, times(2)).saveSuccess(eq(request), any(OmrGradingJobProcessor.Tx.SuccessPrep.class));
        verify(retryTx).finalizeIfDone(1L, 1);
    }

    @Test
    @DisplayName("카운터 반영 전 실패는 replay 가능하도록 dedup을 해제한다")
    void processGradingCallback_uncountedFailure_releasesDedupForReplay() {
        OmrGradingCallbackRequest request = buildSuccessCallback();
        OmrGradingJobProcessor.Tx failingTx = mock(OmrGradingJobProcessor.Tx.class);
        OmrGradingJobProcessor failingProcessor =
                new OmrGradingJobProcessor(storagePort, callbackDeduplication, jobRepository, failingTx);

        given(callbackDeduplication.markIfAbsent("omr:cb:1:0")).willReturn(true);
        given(failingTx.isOpenJob(1L)).willReturn(true);
        doThrow(new BusinessException(ExamErrorCode.EXAM_NOT_FOUND))
                .when(failingTx)
                .markProcessing(1L);

        assertThatThrownBy(() -> failingProcessor.processGradingCallback(request))
                .isInstanceOf(BusinessException.class);

        verify(callbackDeduplication).forget("omr:cb:1:0");
        verify(failingTx, never()).finalizeIfDone(any(), anyInt());
    }

    @Test
    @DisplayName("콜백 실패 시 incrementFail을 호출한다")
    void processGradingCallback_failure() {
        OmrGradingCallbackRequest request = new OmrGradingCallbackRequest(
                1L, 10L, "inst-abc123",
                "omr/tmp/abc123/batch1/sheet1.png",
                0, 1,
                false, "OMR 채점 실패",
                null, 0, 0, null
        );

        given(callbackDeduplication.markIfAbsent(any())).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(eq(1L), eq(List.of(JobStatus.PENDING, JobStatus.PROCESSING))))
                .willReturn(true);
        given(jobRepository.tryFinalize(1L, 1)).willReturn(true);

        processor.processGradingCallback(request);

        verify(jobRepository).incrementFail(1L);
        verify(jobRepository, never()).incrementSuccess(any());
        verify(resultRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("성공 콜백이라도 전화번호가 없으면 실패로 처리한다")
    void processGradingCallback_successWithoutPhone_treatedAsFailure() {
        OmrGradingCallbackRequest request = new OmrGradingCallbackRequest(
                1L, 10L, "inst-abc123",
                "omr/tmp/abc123/batch1/sheet1.png",
                0, 1,
                true, null,
                null, 100, 1,
                List.of()
        );

        given(callbackDeduplication.markIfAbsent(any())).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(eq(1L), eq(List.of(JobStatus.PENDING, JobStatus.PROCESSING))))
                .willReturn(true);
        given(jobRepository.tryFinalize(1L, 1)).willReturn(true);

        processor.processGradingCallback(request);

        verify(jobRepository).incrementFail(1L);
        verify(jobRepository, never()).incrementSuccess(any());
        verify(resultRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("terminal 상태 job의 늦은 콜백은 결과와 카운터를 건드리지 않는다")
    void processGradingCallback_lateCallbackForTerminalJob_skipsMutation() {
        OmrGradingCallbackRequest request = buildSuccessCallback();

        given(callbackDeduplication.markIfAbsent(any())).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(eq(1L), eq(List.of(JobStatus.PENDING, JobStatus.PROCESSING))))
                .willReturn(false);

        processor.processGradingCallback(request);

        verify(jobRepository, never()).startProcessingIfPending(any());
        verify(jobRepository, never()).incrementSuccess(any());
        verify(jobRepository, never()).incrementFail(any());
        verify(jobRepository, never()).tryFinalize(any(), anyInt());
        verify(resultRepository, never()).saveAndFlush(any());
        verify(storagePort, never()).getObject(any());
    }
}
