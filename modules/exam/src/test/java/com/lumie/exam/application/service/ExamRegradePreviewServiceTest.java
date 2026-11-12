package com.lumie.exam.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.application.dto.request.ExamAnswerRegradePreviewRequest;
import com.lumie.exam.application.port.out.ExamPersistencePort;
import com.lumie.exam.application.port.out.ExamAnswerKeyRevisionPersistencePort;
import com.lumie.exam.application.port.out.ExamResultPersistencePort;
import com.lumie.exam.application.port.out.OmrGradingJobPersistencePort;
import com.lumie.exam.application.port.out.OmrSubmissionPersistencePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.OmrSubmission;
import com.lumie.exam.domain.entity.OmrSubmissionQuestion;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import com.lumie.exam.domain.vo.JobStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamRegradePreviewServiceTest {

    @Mock ExamPersistencePort examRepository;
    @Mock ExamResultPersistencePort resultRepository;
    @Mock OmrSubmissionPersistencePort submissionRepository;
    @Mock ExamAnswerKeyRevisionPersistencePort revisionRepository;
    @Mock OmrGradingJobPersistencePort jobRepository;
    @Mock GradeCalculator gradeCalculator;
    @Mock ExamRegradeCalculator calculator;
    @InjectMocks ExamRegradeService service;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void previewShowsProjectedScoreImpactWithoutWriting() {
        TenantContextHolder.setTenantId(101L);
        Exam exam = Exam.create("Regrade", ExamCategory.GRADED, GradingType.ABSOLUTE, GradeScale.NINE_GRADE,
                1, Map.of("1", "2"), Map.of("1", 100), Map.of("1", "CHOICE"), null);
        ExamResult result = ExamResult.create(exam, 7L, 0);
        result.addQuestionResult(QuestionResult.create(1, "4", "2", 100));
        when(examRepository.findById(5L)).thenReturn(Optional.of(exam));
        when(resultRepository.findByExamId(5L)).thenReturn(List.of(result));
        when(submissionRepository.findSucceededByExamId(5L)).thenReturn(List.of());
        when(calculator.calculate(exam, Map.of(1, "4"), Map.of("1", "4")))
                .thenReturn(new ExamRegradeCalculator.Outcome(100, 1,
                        List.of(new ExamRegradeCalculator.QuestionOutcome(1, "4", "4", 100, 100))));

        var preview = service.preview(5L, new ExamAnswerRegradePreviewRequest(Map.of("1", "4")));

        assertThat(preview.regradedResultCount()).isEqualTo(1);
        assertThat(preview.skippedManualOverrideCount()).isZero();
        assertThat(preview.updatedSubmissionCount()).isZero();
        assertThat(preview.scoreImpact()).containsEntry(100, 1L);
        verify(examRepository, never()).save(any());
        verify(resultRepository, never()).save(any());
        verify(submissionRepository, never()).saveAll(any());
    }

    @Test
    void previewBlocksWhileBatchOmrJobIsPendingOrProcessing() {
        TenantContextHolder.setTenant("test-tenant");
        TenantContextHolder.setTenantId(101L);
        Exam exam = Exam.create("Regrade", ExamCategory.GRADED, GradingType.ABSOLUTE, GradeScale.NINE_GRADE,
                1, Map.of("1", "2"), Map.of("1", 100), Map.of("1", "CHOICE"), null);
        when(examRepository.findById(5L)).thenReturn(Optional.of(exam));
        when(jobRepository.existsByExamIdAndStatusIn(
                5L, List.of(JobStatus.PENDING, JobStatus.PROCESSING))).thenReturn(true);

        assertThatThrownBy(() -> service.preview(
                5L, new ExamAnswerRegradePreviewRequest(Map.of("1", "4"))))
                .isInstanceOf(BusinessException.class)
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ExamErrorCode.EXAM_HAS_ACTIVE_OMR_JOB));
    }

    @Test
    void unchangedAnswerPreviewIsReadOnlyAndHasNoImpact() {
        TenantContextHolder.setTenantId(101L);
        Exam exam = exam();
        when(examRepository.findById(5L)).thenReturn(Optional.of(exam));
        when(resultRepository.findByExamId(5L)).thenReturn(List.of());
        when(submissionRepository.findSucceededByExamId(5L)).thenReturn(List.of());

        var preview = service.preview(5L, new ExamAnswerRegradePreviewRequest(Map.of("1", "2")));

        assertThat(preview.noOp()).isTrue();
        assertThat(preview.changedAnswers()).isEmpty();
        verifyNoInteractions(calculator, revisionRepository);
    }

    @Test
    void manualOverridePreviewCountsSkipWithoutChangingStoredScore() {
        TenantContextHolder.setTenantId(101L);
        Exam exam = exam();
        ExamResult result = ExamResult.create(exam, 7L, 70);
        result.assignGrade(3);
        ReflectionTestUtils.setField(result, "manualOverrideAt", Instant.now());
        when(examRepository.findById(5L)).thenReturn(Optional.of(exam));
        when(resultRepository.findByExamId(5L)).thenReturn(List.of(result));
        when(submissionRepository.findSucceededByExamId(5L)).thenReturn(List.of());

        var preview = service.preview(5L, new ExamAnswerRegradePreviewRequest(Map.of("1", "4")));

        assertThat(preview.skippedManualOverrideCount()).isEqualTo(1);
        assertThat(preview.regradedResultCount()).isZero();
        assertThat(result.getTotalScore()).isEqualTo(70);
        verifyNoInteractions(calculator);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "01", "one", "2"})
    void previewRejectsInvalidQuestionNumbers(String question) {
        when(examRepository.findById(5L)).thenReturn(Optional.of(exam()));

        assertThatThrownBy(() -> service.preview(5L,
                new ExamAnswerRegradePreviewRequest(Map.of(question, "4"))))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(resultRepository, submissionRepository, calculator);
    }

    @Test
    void previewRejectsMissingRequestBeforeReadingExam() {
        assertThatThrownBy(() -> service.preview(5L, null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.preview(5L, new ExamAnswerRegradePreviewRequest(null)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(examRepository);
    }

    @Test
    void previewRejectsBlankOrOverlongProposedAnswer() {
        when(examRepository.findById(5L)).thenReturn(Optional.of(exam()));
        for (String value : List.of(" ", "123456789012345678901")) {
            assertThatThrownBy(() -> service.preview(5L,
                    new ExamAnswerRegradePreviewRequest(Map.of("1", value))))
                    .isInstanceOf(BusinessException.class);
        }
        verifyNoInteractions(resultRepository, submissionRepository);
    }

    @Test
    void previewReportsGradeTransitionWithoutPersisting() {
        TenantContextHolder.setTenantId(101L);
        Exam exam = exam();
        ExamResult result = ExamResult.create(exam, 7L, 0);
        result.assignGrade(9);
        result.addQuestionResult(QuestionResult.create(1, "4", "2", 100));
        when(examRepository.findById(5L)).thenReturn(Optional.of(exam));
        when(resultRepository.findByExamId(5L)).thenReturn(List.of(result));
        when(submissionRepository.findSucceededByExamId(5L)).thenReturn(List.of());
        when(calculator.calculate(exam, Map.of(1, "4"), Map.of("1", "4")))
                .thenReturn(new ExamRegradeCalculator.Outcome(100, 1,
                        List.of(new ExamRegradeCalculator.QuestionOutcome(1, "4", "4", 100, 100))));
        when(gradeCalculator.resolveStoredOrRelativeGrade(eq(exam), any(), anyList())).thenReturn(9, 1);

        var preview = service.preview(5L, new ExamAnswerRegradePreviewRequest(Map.of("1", "4")));

        assertThat(preview.gradeImpact()).containsEntry("9->1", 1L);
        verify(examRepository, never()).save(any());
        verify(resultRepository, never()).saveAll(any());
    }

    @Test
    void previewValidatesCompletedOmrAndReportsUnchangedScore() {
        TenantContextHolder.setTenantId(101L);
        Exam exam = exam();
        ExamResult result = ExamResult.create(exam, 7L, 0);
        result.addQuestionResult(QuestionResult.create(1, "2", "2", 100));
        OmrSubmission submission = OmrSubmission.pending(exam, 20L, 0, "source", Instant.now());
        submission.complete(null, 0, 9, "image",
                List.of(OmrSubmissionQuestion.create(1, "2", "2", 100, 0, "CHOICE")));
        when(examRepository.findById(5L)).thenReturn(Optional.of(exam));
        when(resultRepository.findByExamId(5L)).thenReturn(List.of(result));
        when(submissionRepository.findSucceededByExamId(5L)).thenReturn(List.of(submission));
        when(calculator.calculate(exam, Map.of(1, "2"), Map.of("1", "4")))
                .thenReturn(new ExamRegradeCalculator.Outcome(0, 9,
                        List.of(new ExamRegradeCalculator.QuestionOutcome(1, "2", "4", 100, 0))));

        var preview = service.preview(5L, new ExamAnswerRegradePreviewRequest(Map.of("1", "4")));

        assertThat(preview.scoreImpact()).isEmpty();
        assertThat(preview.updatedSubmissionCount()).isEqualTo(1);
        verify(calculator, times(2)).calculate(exam, Map.of(1, "2"), Map.of("1", "4"));
    }

    @Test
    void previewRejectsDuplicateQuestionRowsInSavedResult() {
        TenantContextHolder.setTenantId(101L);
        Exam exam = exam();
        ExamResult result = ExamResult.create(exam, 7L, 0);
        result.addQuestionResult(QuestionResult.create(1, "2", "2", 100));
        result.addQuestionResult(QuestionResult.create(1, "2", "2", 100));
        when(examRepository.findById(5L)).thenReturn(Optional.of(exam));
        when(resultRepository.findByExamId(5L)).thenReturn(List.of(result));
        when(submissionRepository.findSucceededByExamId(5L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.preview(5L, new ExamAnswerRegradePreviewRequest(Map.of("1", "4"))))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(calculator);
    }

    @Test
    void previewRejectsNullProposedAnswer() {
        Map<String, String> changes = new HashMap<>();
        changes.put("1", null);
        when(examRepository.findById(5L)).thenReturn(Optional.of(exam()));

        assertThatThrownBy(() -> service.preview(5L, new ExamAnswerRegradePreviewRequest(changes)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(resultRepository, submissionRepository);
    }

    @Test
    void previewRejectsDuplicateQuestionRowsInCompletedOmr() {
        TenantContextHolder.setTenantId(101L);
        Exam exam = exam();
        OmrSubmission submission = OmrSubmission.pending(exam, 20L, 0, "source", Instant.now());
        submission.complete(null, 0, 9, "image", List.of(
                OmrSubmissionQuestion.create(1, "2", "2", 100, 0, "CHOICE"),
                OmrSubmissionQuestion.create(1, "2", "2", 100, 0, "CHOICE")));
        when(examRepository.findById(5L)).thenReturn(Optional.of(exam));
        when(resultRepository.findByExamId(5L)).thenReturn(List.of());
        when(submissionRepository.findSucceededByExamId(5L)).thenReturn(List.of(submission));

        assertThatThrownBy(() -> service.preview(5L, new ExamAnswerRegradePreviewRequest(Map.of("1", "4"))))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(calculator);
    }

    private static Exam exam() {
        return Exam.create("Regrade", ExamCategory.GRADED, GradingType.ABSOLUTE, GradeScale.NINE_GRADE,
                1, Map.of("1", "2"), Map.of("1", 100), Map.of("1", "CHOICE"), null);
    }
}
