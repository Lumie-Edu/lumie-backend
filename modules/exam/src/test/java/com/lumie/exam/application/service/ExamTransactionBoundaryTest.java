package com.lumie.exam.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.api.ExamResultDeletingEvent;
import com.lumie.exam.application.port.out.ExamResultPersistencePort;
import com.lumie.exam.application.port.out.OmrGradingJobPersistencePort;
import com.lumie.exam.application.port.out.OmrSubmissionPersistencePort;
import com.lumie.exam.application.port.out.ReportGenerationJobPersistencePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.OmrSubmission;
import com.lumie.exam.domain.exception.ExamErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ExamTransactionBoundaryTest {

    @Mock
    private ExamResultPersistencePort results;

    @Mock
    private OmrSubmissionPersistencePort submissions;

    @Mock
    private ApplicationEventPublisher events;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void resultDeletionReturnsImageKeyAfterPersistedRowAndEvent() {
        ExamResult result = mock(ExamResult.class);
        Exam exam = mock(Exam.class);
        given(results.findById(22L)).willReturn(Optional.of(result));
        given(result.getExam()).willReturn(exam);
        given(exam.getId()).willReturn(11L);
        given(result.getOmrImageKey()).willReturn("tenant/exam/image.png");

        String key = new ResultCommandService.Tx(results, events).deleteResult(11L, 22L);

        assertThat(key).isEqualTo("tenant/exam/image.png");
        verify(events).publishEvent(new ExamResultDeletingEvent(1L, 11L, 22L));
        verify(results).deleteById(22L);
    }

    @Test
    void resultDeletionRejectsMissingAndForeignExamBeforeMutation() {
        given(results.findById(22L)).willReturn(Optional.empty());
        assertResultNotFound(() -> new ResultCommandService.Tx(results, events).deleteResult(11L, 22L));
        verifyNoInteractions(events);
        verify(results, never()).deleteById(22L);

        ExamResult foreign = mock(ExamResult.class);
        Exam exam = mock(Exam.class);
        given(results.findById(23L)).willReturn(Optional.of(foreign));
        given(foreign.getExam()).willReturn(exam);
        given(exam.getId()).willReturn(99L);
        assertResultNotFound(() -> new ResultCommandService.Tx(results, events).deleteResult(11L, 23L));
        verify(results, never()).deleteById(23L);
    }

    @Test
    void imageKeyLookupReturnsKeyOnlyForMatchingExam() {
        OmrSubmission submission = mock(OmrSubmission.class);
        Exam exam = mock(Exam.class);
        given(submissions.findById(22L)).willReturn(Optional.of(submission));
        given(submission.getExam()).willReturn(exam);
        given(exam.getId()).willReturn(11L);
        given(submission.getOmrImageKey()).willReturn("tenant/exam/omr.png");

        assertThat(new OmrSubmissionService.Tx(submissions).loadImageKey(11L, 22L))
                .isEqualTo("tenant/exam/omr.png");
    }

    @Test
    void imageKeyLookupRejectsMissingSubmissionForeignExamAndMissingImage() {
        given(submissions.findById(22L)).willReturn(Optional.empty());
        assertResultNotFound(() -> new OmrSubmissionService.Tx(submissions).loadImageKey(11L, 22L));

        OmrSubmission foreign = mock(OmrSubmission.class);
        Exam foreignExam = mock(Exam.class);
        given(submissions.findById(23L)).willReturn(Optional.of(foreign));
        given(foreign.getExam()).willReturn(foreignExam);
        given(foreignExam.getId()).willReturn(99L);
        assertResultNotFound(() -> new OmrSubmissionService.Tx(submissions).loadImageKey(11L, 23L));

        OmrSubmission noImage = mock(OmrSubmission.class);
        Exam exam = mock(Exam.class);
        given(submissions.findById(24L)).willReturn(Optional.of(noImage));
        given(noImage.getExam()).willReturn(exam);
        given(exam.getId()).willReturn(11L);
        assertThatThrownBy(() -> new OmrSubmissionService.Tx(submissions).loadImageKey(11L, 24L))
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(ExamErrorCode.OMR_IMAGE_NOT_FOUND));
    }

    @Test
    void conditionalJobTransitionsReturnFalseWhenNoRowChanges() {
        OmrGradingJobPersistencePort omr = mock(OmrGradingJobPersistencePort.class, CALLS_REAL_METHODS);
        ReportGenerationJobPersistencePort report = mock(ReportGenerationJobPersistencePort.class, CALLS_REAL_METHODS);

        assertThat(omr.startProcessingIfPending(31L)).isFalse();
        assertThat(omr.tryFinalize(31L, 2)).isFalse();
        assertThat(report.startProcessingIfPending(41L)).isFalse();
        assertThat(report.tryFinalize(41L, 2)).isFalse();
    }

    private static void assertResultNotFound(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(BusinessException.class, error ->
                        assertThat(error.getErrorCode()).isEqualTo(ExamErrorCode.RESULT_NOT_FOUND));
    }
}
