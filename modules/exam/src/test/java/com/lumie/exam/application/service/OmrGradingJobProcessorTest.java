package com.lumie.exam.application.service;

import com.lumie.common.test.TenantContextExtension;
import com.lumie.exam.application.dto.request.OmrGradingCallbackRequest;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.exam.domain.vo.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class, TenantContextExtension.class})
@DisplayName("OmrGradingJobProcessor 단위 테스트")
class OmrGradingJobProcessorTest {

    @Mock private OmrStoragePort storagePort;
    @Mock private OmrGradingJobRepository jobRepository;
    @Mock private OmrSubmissionService submissionService;

    private OmrGradingJobProcessor processor;

    @BeforeEach
    void setUp() {
        OmrGradingJobProcessor.Tx tx = new OmrGradingJobProcessor.Tx(jobRepository, submissionService);
        processor = new OmrGradingJobProcessor(storagePort, jobRepository, tx);
    }

    private OmrGradingCallbackRequest successCallback() {
        return new OmrGradingCallbackRequest(
                1L, 10L, "inst-abc123", "tmp/abc123/omr/batch/sheet.png", 0, 1,
                true, null, "01012345678", 100, 1,
                List.of(new OmrGradingCallbackRequest.QuestionResultItem(1, "1", "1", 100, 100, "SHORT")));
    }

    @Test
    @DisplayName("PENDING 제출본의 성공 callback은 한 번만 저장하고 카운터를 증가시킨다")
    void processGradingCallback_successTransitionsPendingSubmissionOnce() {
        OmrGradingCallbackRequest request = successCallback();
        var submission = org.mockito.Mockito.mock(com.lumie.exam.domain.entity.OmrSubmission.class);
        given(submission.getId()).willReturn(55L);
        given(jobRepository.existsByIdAndStatusIn(1L, List.of(JobStatus.PENDING, JobStatus.PROCESSING))).willReturn(true);
        given(submissionService.getSubmission(1L, 0)).willReturn(submission);
        given(submissionService.completeIfPending(1L, 0, request, "abc123/omr/submissions/55.png")).willReturn(true);
        given(storagePort.getObject(any())).willReturn(new byte[] {1});
        given(storagePort.putObject(any(), any(), any())).willReturn("abc123/omr/submissions/55.png");

        processor.processGradingCallback(request);

        verify(submissionService).completeIfPending(1L, 0, request, "abc123/omr/submissions/55.png");
        verify(jobRepository).incrementSuccess(1L);
        verify(jobRepository).tryFinalize(1L, 1);
    }

    @Test
    @DisplayName("중복 callback은 제출본과 카운터를 다시 변경하지 않고 완료 판정만 재시도한다")
    void processGradingCallback_duplicateDoesNotRecount() {
        OmrGradingCallbackRequest request = successCallback();
        OmrGradingJobProcessor.Tx tx = org.mockito.Mockito.mock(OmrGradingJobProcessor.Tx.class);
        OmrGradingJobProcessor duplicateProcessor = new OmrGradingJobProcessor(storagePort, jobRepository, tx);
        OmrGradingJobProcessor.Tx.SuccessPrep prep = new OmrGradingJobProcessor.Tx.SuccessPrep("abc123/omr/submissions/55.png");
        given(tx.isOpenJob(1L)).willReturn(true);
        given(tx.prepareSuccess(request)).willReturn(prep);
        given(storagePort.getObject(any())).willReturn(new byte[] {1});
        given(storagePort.putObject(any(), any(), any())).willReturn("abc123/omr/submissions/55.png");
        given(tx.saveSuccess(request, prep, "abc123/omr/submissions/55.png")).willReturn(false);

        duplicateProcessor.processGradingCallback(request);

        verify(tx).saveSuccess(request, prep, "abc123/omr/submissions/55.png");
        verify(tx).finalizeIfDone(1L, 1);
        verify(jobRepository, never()).incrementSuccess(any());
    }

    @Test
    @DisplayName("실패 callback은 PENDING 제출본을 FAILED로 전이시키고 실패 카운터를 증가시킨다")
    void processGradingCallback_failureTransitionsPendingSubmissionOnce() {
        OmrGradingCallbackRequest request = new OmrGradingCallbackRequest(
                1L, 10L, "inst-abc123", "tmp/abc123/omr/batch/sheet.png", 0, 1,
                false, "OMR 채점 실패", null, 0, 0, null);
        given(jobRepository.existsByIdAndStatusIn(1L, List.of(JobStatus.PENDING, JobStatus.PROCESSING))).willReturn(true);
        given(submissionService.failIfPending(1L, 0, "OMR 채점 실패")).willReturn(true);

        processor.processGradingCallback(request);

        verify(submissionService).failIfPending(1L, 0, "OMR 채점 실패");
        verify(jobRepository).incrementFail(1L);
    }

    @Test
    @DisplayName("terminal job의 늦은 callback은 저장이나 이미지 복사를 수행하지 않는다")
    void processGradingCallback_terminalJobSkipsMutation() {
        given(jobRepository.existsByIdAndStatusIn(1L, List.of(JobStatus.PENDING, JobStatus.PROCESSING))).willReturn(false);

        processor.processGradingCallback(successCallback());

        verify(submissionService, never()).completeIfPending(any(), anyInt(), any(), any());
        verify(storagePort, never()).getObject(any());
    }
}
