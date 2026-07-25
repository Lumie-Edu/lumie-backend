package com.lumie.exam.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.test.TenantContextExtension;
import com.lumie.exam.application.dto.request.OmrGradingCallbackRequest;
import com.lumie.exam.application.port.out.CallbackDeduplicationPort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.exam.domain.vo.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, TenantContextExtension.class})
@DisplayName("OmrGradingJobProcessor 단위 테스트")
class OmrGradingJobProcessorTest {

    @Mock private OmrStoragePort storagePort;
    @Mock private CallbackDeduplicationPort callbackDeduplication;
    @Mock private OmrGradingJobRepository jobRepository;
    @Mock private OmrSubmissionService submissionService;

    private OmrGradingJobProcessor processor;

    @BeforeEach
    void setUp() {
        OmrGradingJobProcessor.Tx tx = new OmrGradingJobProcessor.Tx(jobRepository, submissionService);
        processor = new OmrGradingJobProcessor(storagePort, callbackDeduplication, jobRepository, tx);
    }

    private OmrGradingCallbackRequest successCallback(String phoneNumber) {
        return new OmrGradingCallbackRequest(
                1L, 10L, "inst-abc123", "tmp/abc123/omr/batch/sheet.png", 0, 1,
                true, null, phoneNumber, 100, 1,
                List.of(new OmrGradingCallbackRequest.QuestionResultItem(1, "1", "1", 100, 100, "SHORT")));
    }

    @Test
    @DisplayName("성공 callback은 submission의 고유 영구 이미지 키로 완료 처리한다")
    void processGradingCallback_successCompletesSubmission() {
        OmrGradingCallbackRequest request = successCallback("12345678");
        var submission = mock(com.lumie.exam.domain.entity.OmrSubmission.class);
        given(submission.getId()).willReturn(55L);
        given(callbackDeduplication.markIfAbsent("omr:cb:1:0")).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(1L, List.of(JobStatus.PENDING, JobStatus.PROCESSING))).willReturn(true);
        given(submissionService.getSubmission(1L, 0)).willReturn(submission);
        given(storagePort.getObject(any())).willReturn(new byte[] {1});
        given(storagePort.putObject(any(), any(), any())).willReturn("abc123/omr/submissions/55.png");
        given(jobRepository.tryFinalize(1L, 1)).willReturn(true);

        processor.processGradingCallback(request);

        verify(submissionService).complete(1L, 0, request, "abc123/omr/submissions/55.png");
        verify(jobRepository).incrementSuccess(1L);
    }

    @Test
    @DisplayName("OCR 전화번호가 없는 성공 callback도 submission으로 보존한다")
    void processGradingCallback_successWithoutPhoneIsNotDropped() {
        OmrGradingCallbackRequest request = successCallback(null);
        var submission = mock(com.lumie.exam.domain.entity.OmrSubmission.class);
        given(submission.getId()).willReturn(56L);
        given(callbackDeduplication.markIfAbsent(any())).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(1L, List.of(JobStatus.PENDING, JobStatus.PROCESSING))).willReturn(true);
        given(submissionService.getSubmission(1L, 0)).willReturn(submission);
        given(storagePort.getObject(any())).willReturn(new byte[] {1});
        given(storagePort.putObject(any(), any(), any())).willReturn("abc123/omr/submissions/56.png");
        given(jobRepository.tryFinalize(1L, 1)).willReturn(true);

        processor.processGradingCallback(request);

        verify(submissionService).complete(1L, 0, request, "abc123/omr/submissions/56.png");
        verify(jobRepository).incrementSuccess(1L);
        verify(jobRepository, never()).incrementFail(1L);
    }

    @Test
    @DisplayName("실패 callback은 해당 submission을 FAILED로 남긴다")
    void processGradingCallback_failureMarksSubmissionFailed() {
        OmrGradingCallbackRequest request = new OmrGradingCallbackRequest(
                1L, 10L, "inst-abc123", "tmp/abc123/omr/batch/sheet.png", 0, 1,
                false, "OMR 채점 실패", null, 0, 0, null);
        given(callbackDeduplication.markIfAbsent(any())).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(1L, List.of(JobStatus.PENDING, JobStatus.PROCESSING))).willReturn(true);
        given(jobRepository.tryFinalize(1L, 1)).willReturn(true);

        processor.processGradingCallback(request);

        verify(submissionService).fail(1L, 0, "OMR 채점 실패");
        verify(jobRepository).incrementFail(1L);
    }

    @Test
    @DisplayName("영구 이미지 복사 실패는 submission을 FAILED로 남기고 임시 원본을 삭제하지 않는다")
    void processGradingCallback_copyFailurePreservesSourceImage() {
        OmrGradingCallbackRequest request = successCallback("12345678");
        var submission = mock(com.lumie.exam.domain.entity.OmrSubmission.class);
        var job = mock(com.lumie.exam.domain.entity.OmrGradingJob.class);
        given(submission.getId()).willReturn(57L);
        given(callbackDeduplication.markIfAbsent("omr:cb:1:0")).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(1L, List.of(JobStatus.PENDING, JobStatus.PROCESSING))).willReturn(true);
        given(submissionService.getSubmission(1L, 0)).willReturn(submission);
        given(storagePort.getObject(any())).willThrow(new IllegalStateException("MinIO unavailable"));
        given(jobRepository.tryFinalize(1L, 1)).willReturn(true);
        given(jobRepository.findById(1L)).willReturn(java.util.Optional.of(job));
        given(job.getImageKeys()).willReturn(List.of(request.imageKey()));
        given(submissionService.getSourceImageKeysWithoutPermanentCopy(1L)).willReturn(List.of(request.imageKey()));

        processor.processGradingCallback(request);

        verify(submissionService).fail(1L, 0, "IMAGE_PERSISTENCE_FAILED");
        verify(jobRepository).incrementFail(1L);
        verify(submissionService, never()).complete(any(), anyInt(), any(), any());
        verify(storagePort, never()).deleteObjects(any());
    }

    @Test
    @DisplayName("일반 채점 실패의 임시 원본은 작업 완료 후 정리한다")
    void processGradingCallback_gradingFailureCleansSourceImage() {
        OmrGradingCallbackRequest request = new OmrGradingCallbackRequest(
                1L, 10L, "inst-abc123", "tmp/abc123/omr/batch/sheet.png", 0, 1,
                false, "OMR 채점 실패", null, 0, 0, null);
        var job = mock(com.lumie.exam.domain.entity.OmrGradingJob.class);
        given(callbackDeduplication.markIfAbsent(any())).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(1L, List.of(JobStatus.PENDING, JobStatus.PROCESSING))).willReturn(true);
        given(jobRepository.tryFinalize(1L, 1)).willReturn(true);
        given(jobRepository.findById(1L)).willReturn(java.util.Optional.of(job));
        given(job.getImageKeys()).willReturn(List.of(request.imageKey()));
        given(submissionService.getSourceImageKeysWithoutPermanentCopy(1L)).willReturn(List.of());

        processor.processGradingCallback(request);

        verify(storagePort).deleteObjects(List.of(request.imageKey()));
    }

    @Test
    @DisplayName("optimistic lock 실패는 dedup을 해제한 뒤 재시도한다")
    void processGradingCallback_optimisticLockFailure_releasesDedupAndRetries() {
        OmrGradingCallbackRequest request = successCallback("12345678");
        OmrGradingJobProcessor.Tx retryTx = mock(OmrGradingJobProcessor.Tx.class);
        OmrGradingJobProcessor retryingProcessor = new OmrGradingJobProcessor(
                storagePort, callbackDeduplication, jobRepository, retryTx);
        OmrGradingJobProcessor.Tx.SuccessPrep prep = new OmrGradingJobProcessor.Tx.SuccessPrep("abc123/omr/submissions/55.png");
        given(callbackDeduplication.markIfAbsent("omr:cb:1:0")).willReturn(true, true);
        given(retryTx.isOpenJob(1L)).willReturn(true);
        given(retryTx.prepareSuccess(request)).willReturn(prep);
        given(storagePort.getObject(any())).willReturn(new byte[] {1});
        given(storagePort.putObject(any(), any(), any())).willReturn("abc123/omr/submissions/55.png");
        doThrow(new OptimisticLockingFailureException("stale"))
                .doNothing()
                .when(retryTx).saveSuccess(eq(request), eq(prep), any());
        given(retryTx.finalizeIfDone(1L, 1)).willReturn(true);

        retryingProcessor.processGradingCallback(request);

        verify(callbackDeduplication).forget("omr:cb:1:0");
        verify(retryTx, times(2)).saveSuccess(eq(request), eq(prep), any());
    }

    @Test
    @DisplayName("terminal 상태 job의 늦은 callback은 mutation하지 않는다")
    void processGradingCallback_lateCallbackSkipsMutation() {
        given(callbackDeduplication.markIfAbsent(any())).willReturn(true);
        given(jobRepository.existsByIdAndStatusIn(1L, List.of(JobStatus.PENDING, JobStatus.PROCESSING))).willReturn(false);

        processor.processGradingCallback(successCallback("12345678"));

        verify(submissionService, never()).complete(any(), anyInt(), any(), any());
        verify(jobRepository, never()).incrementSuccess(any());
        verify(storagePort, never()).getObject(any());
    }

    @Test
    @DisplayName("중복 callback은 재계산 없이 완료 처리를 재시도한다")
    void processGradingCallback_duplicateRetriesFinalizationWithoutRecounting() {
        OmrGradingCallbackRequest request = successCallback("12345678");
        given(callbackDeduplication.markIfAbsent("omr:cb:1:0")).willReturn(false);
        given(jobRepository.tryFinalize(1L, 1)).willReturn(true);

        processor.processGradingCallback(request);

        verify(jobRepository).tryFinalize(1L, 1);
        verify(jobRepository, never()).incrementSuccess(any());
        verify(submissionService, never()).complete(any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("callback 처리 전 실패는 dedup을 해제한다")
    void processGradingCallback_uncommittedFailureReleasesDedup() {
        OmrGradingJobProcessor.Tx failingTx = mock(OmrGradingJobProcessor.Tx.class);
        OmrGradingJobProcessor failingProcessor = new OmrGradingJobProcessor(
                storagePort, callbackDeduplication, jobRepository, failingTx);
        given(callbackDeduplication.markIfAbsent(any())).willReturn(true);
        given(failingTx.isOpenJob(1L)).willReturn(true);
        doThrow(new BusinessException(ExamErrorCode.EXAM_NOT_FOUND)).when(failingTx).markProcessing(1L);

        assertThatThrownBy(() -> failingProcessor.processGradingCallback(successCallback("12345678")))
                .isInstanceOf(BusinessException.class);

        verify(callbackDeduplication).forget("omr:cb:1:0");
    }
}
