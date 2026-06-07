package com.lumie.exam.application.service;

import com.lumie.common.test.TenantContextExtension;
import com.lumie.exam.application.dto.request.OmrGradingCallbackRequest;
import com.lumie.exam.application.port.out.CallbackDeduplicationPort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.student.api.StudentService;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.exam.domain.vo.JobStatus;
import com.lumie.exam.test.ExamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        given(resultRepository.save(any(ExamResult.class))).willReturn(resolvedResult);
        given(jobRepository.tryFinalize(1L, 1)).willReturn(true);

        processor.processGradingCallback(request);

        verify(jobRepository).incrementSuccess(1L);
        verify(resultRepository).save(any(ExamResult.class));
        // 가입 학생도 채점 결과에 전화번호가 저장되어야 한다 (010 + 콜백 phoneNumber).
        assertThat(resolvedResult.getPhoneNumber()).isEqualTo("01012345678");
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
        verify(resultRepository, never()).save(any());
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
        verify(resultRepository, never()).save(any());
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
        verify(resultRepository, never()).save(any());
        verify(storagePort, never()).getObject(any());
    }
}
