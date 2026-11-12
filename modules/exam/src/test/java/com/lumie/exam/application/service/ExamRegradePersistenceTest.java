package com.lumie.exam.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.request.ExamAnswerRegradeConfirmRequest;
import com.lumie.exam.application.port.out.ExamAnswerKeyRevisionPersistencePort;
import com.lumie.exam.application.port.out.ExamPersistencePort;
import com.lumie.exam.application.port.out.ExamResultPersistencePort;
import com.lumie.exam.application.port.out.OmrGradingJobPersistencePort;
import com.lumie.exam.application.port.out.OmrSubmissionPersistencePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamAnswerKeyRevision;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.OmrSubmission;
import com.lumie.exam.domain.entity.OmrSubmissionQuestion;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import com.lumie.exam.domain.vo.OmrReconciliationStatus;
import com.lumie.exam.application.service.ExamRegradeCalculator.Outcome;
import com.lumie.exam.application.service.ExamRegradeCalculator.QuestionOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamRegradePersistenceTest {

    @Mock ExamPersistencePort examRepository;
    @Mock ExamResultPersistencePort resultRepository;
    @Mock OmrSubmissionPersistencePort submissionRepository;
    @Mock OmrGradingJobPersistencePort omrGradingJobRepository;
    @Mock ExamAnswerKeyRevisionPersistencePort revisionRepository;
    @Mock GradeCalculator gradeCalculator;
    @Mock ExamRegradeCalculator calculator;
    @InjectMocks ExamRegradeService service;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void confirmationUpdatesExamOfficialResultAndCompletedOmrSnapshotAtomically() {
        TenantContextHolder.setTenant("test-tenant");
        TenantContextHolder.setTenantId(101L);
        Exam exam = exam();
        ExamResult result = ExamResult.create(exam, 20L, 0);
        result.assignGrade(9);
        result.addQuestionResult(QuestionResult.create(1, "4", "2", 100));
        ReflectionTestUtils.setField(result, "id", 30L);
        ReflectionTestUtils.setField(result, "version", 0L);

        OmrSubmission submission = OmrSubmission.pending(exam, 50L, 0, "source-key", Instant.now());
        submission.complete(null, 0, 9, "omr-key",
                List.of(OmrSubmissionQuestion.create(1, "4", "2", 100, 0, "CHOICE")));
        submission.markUnmatched("NO_ACTIVE_STUDENT_MATCH");
        ReflectionTestUtils.setField(submission, "id", 40L);
        ReflectionTestUtils.setField(submission, "version", 0L);

        Outcome outcome = new Outcome(80, 1,
                List.of(new QuestionOutcome(1, "4", "4", 80, 80)));
        when(examRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(exam));
        when(examRepository.save(exam)).thenReturn(exam);
        when(resultRepository.findByExamId(5L)).thenReturn(List.of(result));
        when(submissionRepository.findSucceededByExamId(5L)).thenReturn(List.of(submission));
        when(submissionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(revisionRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(calculator.calculate(exam, Map.of(1, "4"), Map.of("1", "4"))).thenAnswer(invocation -> {
            assertThat(exam.getQuestionScores()).containsEntry("1", 80);
            assertThat(exam.getQuestionTypes()).containsEntry("1", "ESSAY");
            return outcome;
        });

        var receipt = service.confirm(5L,
                new ExamAnswerRegradeConfirmRequest("Updated Regrade", Map.of("1", "4"),
                        Map.of("1", 80), Map.of("1", "ESSAY")), 10L);

        assertThat(receipt.regradedResultCount()).isEqualTo(1);
        assertThat(receipt.updatedSubmissionCount()).isEqualTo(1);
        assertThat(exam.getCorrectAnswers()).containsEntry("1", "4");
        assertThat(exam.getName()).isEqualTo("Updated Regrade");
        assertThat(exam.getQuestionScores()).containsEntry("1", 80);
        assertThat(exam.getQuestionTypes()).containsEntry("1", "ESSAY");
        assertThat(result.getTotalScore()).isEqualTo(80);
        assertThat(result.getGrade()).isEqualTo(1);
        assertThat(result.getQuestionResults().getFirst().isCorrect()).isTrue();
        assertThat(submission.getTotalScore()).isEqualTo(80);
        assertThat(submission.getQuestions().getFirst().getCorrectAnswer()).isEqualTo("4");
        assertThat(submission.getQuestions().getFirst().getEarnedScore()).isEqualTo(80);
        assertThat(submission.getQuestions().getFirst().getQuestionType()).isEqualTo("ESSAY");
        assertThat(receipt.changedFields()).containsKeys("name", "score:1", "type:1");
        assertThat(submission.getReconciliationStatus()).isEqualTo(OmrReconciliationStatus.UNMATCHED);
        verify(revisionRepository).saveAndFlush(any());
    }

    @Test
    void nameOnlyCorrectionRecordsHistoryWithoutRegradingResults() {
        TenantContextHolder.setTenant("test-tenant");
        TenantContextHolder.setTenantId(101L);
        Exam exam = exam();
        when(examRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(exam));
        when(examRepository.save(exam)).thenReturn(exam);
        when(revisionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var receipt = service.confirm(5L,
                new ExamAnswerRegradeConfirmRequest("Renamed exam", Map.of(),
                        Map.of("1", 100), Map.of("1", "CHOICE")), 10L);

        assertThat(exam.getName()).isEqualTo("Renamed exam");
        assertThat(receipt.changedAnswers()).isEmpty();
        assertThat(receipt.changedFields()).containsKey("name");
        assertThat(receipt.regradedResultCount()).isZero();
        assertThat(receipt.updatedSubmissionCount()).isZero();
        verifyNoInteractions(resultRepository, submissionRepository, calculator);
    }

    @Test
    void unchangedConfigurationDoesNotWriteRevision() {
        Exam exam = exam();
        when(examRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(exam));

        var receipt = service.confirm(5L,
                new ExamAnswerRegradeConfirmRequest("Regrade", Map.of(),
                        Map.of("1", 100), Map.of("1", "CHOICE")), 10L);

        assertThat(receipt.noOp()).isTrue();
        verify(examRepository, never()).save(any());
        verifyNoInteractions(resultRepository, submissionRepository, revisionRepository);
    }

    @Test
    void incompleteScoreMapBlocksAllWrites() {
        when(examRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(exam()));

        assertThatThrownBy(() -> service.confirm(5L,
                new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(), Map.of(),
                        Map.of("1", "CHOICE")), 10L))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);
        verify(examRepository, never()).save(any());
        verifyNoInteractions(resultRepository, submissionRepository, revisionRepository);
    }

    @Test
    void activeOmrBatchBlocksMetadataCorrection() {
        when(examRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(exam()));
        when(omrGradingJobRepository.existsByExamIdAndStatusIn(eq(5L), any())).thenReturn(true);

        assertThatThrownBy(() -> service.confirm(5L,
                new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(),
                        Map.of("1", 100), Map.of("1", "CHOICE")), 10L))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);
        verify(examRepository, never()).save(any());
    }

    @Test
    void missingStaffActorCannotConfirmCorrection() {
        assertThatThrownBy(() -> service.confirm(5L,
                new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(),
                        Map.of("1", 100), Map.of("1", "CHOICE")), null))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);
        verifyNoInteractions(examRepository, resultRepository, submissionRepository, revisionRepository);
    }

    @Test
    void historyReturnsPersistedActorChangesAndCounts() {
        Exam exam = exam();
        ExamAnswerKeyRevision revision = ExamAnswerKeyRevision.create(exam, 10L, 0L, 1L,
                Map.of("1", new ExamAnswerKeyRevision.AnswerChange("2", "4")),
                Map.of("name", new ExamAnswerKeyRevision.AnswerChange("Regrade", "Renamed")),
                2, 1, 3);
        var page = PageRequest.of(0, 10);
        when(examRepository.findById(5L)).thenReturn(Optional.of(exam));
        when(revisionRepository.findByExamId(5L, page)).thenReturn(new PageImpl<>(List.of(revision)));

        var history = service.history(5L, page);

        assertThat(history.getContent()).hasSize(1);
        assertThat(history.getContent().getFirst().actorUserId()).isEqualTo(10L);
        assertThat(history.getContent().getFirst().changedFields()).containsKey("name");
    }

    @Test
    void passFailCorrectionCannotLowerTotalBelowPassThreshold() {
        Exam exam = Exam.create("Pass exam", ExamCategory.PASS_FAIL, null, null,
                1, Map.of("1", "2"), Map.of("1", 10), Map.of(), 8);
        when(examRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(exam));

        assertThatThrownBy(() -> service.confirm(5L,
                new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(),
                        Map.of("1", 5), Map.of()), 10L))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);
        verify(examRepository, never()).save(any());
    }

    @Test
    void passFailScoreChangeAboveThresholdIsRecorded() {
        TenantContextHolder.setTenantId(101L);
        Exam exam = Exam.create("Pass exam", ExamCategory.PASS_FAIL, null, null,
                1, Map.of("1", "2"), Map.of("1", 10), Map.of(), 8);
        when(examRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(exam));
        when(resultRepository.findByExamId(5L)).thenReturn(List.of());
        when(submissionRepository.findSucceededByExamId(5L)).thenReturn(List.of());
        when(revisionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var receipt = service.confirm(5L,
                new ExamAnswerRegradeConfirmRequest("Pass exam", Map.of(),
                        Map.of("1", 9), Map.of()), 10L);

        assertThat(receipt.changedFields()).containsKey("score:1");
        assertThat(exam.getQuestionScores()).containsEntry("1", 9);
    }

    @Test
    void unchangedAnswerIsNotRecordedAsAnAnswerChange() {
        TenantContextHolder.setTenantId(101L);
        Exam exam = exam();
        when(examRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(exam));
        when(revisionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var receipt = service.confirm(5L,
                new ExamAnswerRegradeConfirmRequest("Renamed", Map.of("1", "2"),
                        Map.of("1", 100), Map.of("1", "CHOICE")), 10L);

        assertThat(receipt.changedAnswers()).isEmpty();
        assertThat(receipt.changedFields()).containsKey("name");
        verifyNoInteractions(resultRepository, submissionRepository);
    }

    @Test
    void nullAnswerOrScoreInCompleteMapIsRejected() {
        when(examRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(exam()));
        Map<String, String> nullAnswer = new HashMap<>();
        nullAnswer.put("1", null);
        assertThatThrownBy(() -> service.confirm(5L,
                new ExamAnswerRegradeConfirmRequest("Renamed", nullAnswer,
                        Map.of("1", 100), Map.of("1", "CHOICE")), 10L))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);

        Map<String, Integer> nullScore = new HashMap<>();
        nullScore.put("1", null);
        assertThatThrownBy(() -> service.confirm(5L,
                new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(), nullScore,
                        Map.of("1", "CHOICE")), 10L))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);
        verify(examRepository, never()).save(any());
    }

    @Test
    void assignmentOwnedExamCannotUseExamManagementCorrection() {
        Exam assignment = Exam.createForAssignment("Assignment", ExamCategory.GRADED,
                GradingType.ABSOLUTE, GradeScale.NINE_GRADE, 1,
                Map.of("1", "2"), Map.of("1", 100), Map.of("1", "CHOICE"), null);
        when(examRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> service.confirm(5L,
                new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(),
                        Map.of("1", 100), Map.of("1", "CHOICE")), 10L))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);
        verify(examRepository, never()).save(any());
    }

    @Test
    void manualOverrideResultIsSkippedDuringAnswerCorrection() {
        TenantContextHolder.setTenant("test-tenant");
        TenantContextHolder.setTenantId(101L);
        Exam exam = exam();
        ExamResult result = ExamResult.create(exam, 20L, 70);
        ReflectionTestUtils.setField(result, "manualOverrideAt", Instant.now());
        when(examRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(exam));
        when(examRepository.save(exam)).thenReturn(exam);
        when(resultRepository.findByExamId(5L)).thenReturn(List.of(result));
        when(submissionRepository.findSucceededByExamId(5L)).thenReturn(List.of());
        when(revisionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var receipt = service.confirm(5L,
                new ExamAnswerRegradeConfirmRequest("Regrade", Map.of("1", "4"),
                        Map.of("1", 100), Map.of("1", "CHOICE")), 10L);

        assertThat(receipt.skippedManualOverrideCount()).isEqualTo(1);
        assertThat(receipt.regradedResultCount()).isZero();
        assertThat(result.getTotalScore()).isEqualTo(70);
        verify(resultRepository, never()).saveAll(any());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCorrections")
    void invalidCorrectionDoesNotWrite(String caseName, ExamAnswerRegradeConfirmRequest request) {
        if (request != null && request.name() != null && !request.name().isBlank()
                && request.answerChanges() != null && request.questionScores() != null
                && request.questionTypes() != null) {
            when(examRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(exam()));
        }

        assertThatThrownBy(() -> service.confirm(5L, request, 10L))
                .isInstanceOf(com.lumie.common.exception.BusinessException.class);
        verify(examRepository, never()).save(any());
        verifyNoInteractions(resultRepository, submissionRepository, revisionRepository);
    }

    private static Stream<Arguments> invalidCorrections() {
        return Stream.of(
                Arguments.of("missing request", null),
                Arguments.of("blank name", new ExamAnswerRegradeConfirmRequest(" ", Map.of(), Map.of("1", 100), Map.of("1", "CHOICE"))),
                Arguments.of("missing answer changes", new ExamAnswerRegradeConfirmRequest("Renamed", null, Map.of("1", 100), Map.of("1", "CHOICE"))),
                Arguments.of("missing scores", new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(), null, Map.of("1", "CHOICE"))),
                Arguments.of("missing types", new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(), Map.of("1", 100), null)),
                Arguments.of("noncanonical question", new ExamAnswerRegradeConfirmRequest("Renamed", Map.of("01", "4"), Map.of("1", 100), Map.of("1", "CHOICE"))),
                Arguments.of("non-numeric question", new ExamAnswerRegradeConfirmRequest("Renamed", Map.of("one", "4"), Map.of("1", 100), Map.of("1", "CHOICE"))),
                Arguments.of("unknown question", new ExamAnswerRegradeConfirmRequest("Renamed", Map.of("2", "4"), Map.of("1", 100), Map.of("1", "CHOICE"))),
                Arguments.of("blank answer", new ExamAnswerRegradeConfirmRequest("Renamed", Map.of("1", " "), Map.of("1", 100), Map.of("1", "CHOICE"))),
                Arguments.of("overlong answer", new ExamAnswerRegradeConfirmRequest("Renamed", Map.of("1", "123456789012345678901"), Map.of("1", 100), Map.of("1", "CHOICE"))),
                Arguments.of("negative score", new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(), Map.of("1", -1), Map.of("1", "CHOICE"))),
                Arguments.of("oversized score", new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(), Map.of("1", 101), Map.of("1", "CHOICE"))),
                Arguments.of("noncanonical score key", new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(), Map.of("01", 100), Map.of("1", "CHOICE"))),
                Arguments.of("noncanonical type key", new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(), Map.of("1", 100), Map.of("01", "CHOICE"))),
                Arguments.of("missing graded type", new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(), Map.of("1", 100), Map.of())),
                Arguments.of("blank graded type", new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(), Map.of("1", 100), Map.of("1", " "))),
                Arguments.of("overlong graded type", new ExamAnswerRegradeConfirmRequest("Renamed", Map.of(), Map.of("1", 100), Map.of("1", "T".repeat(51))))
        );
    }

    private static Exam exam() {
        Exam exam = Exam.create("Regrade", ExamCategory.GRADED, GradingType.ABSOLUTE, GradeScale.NINE_GRADE,
                1, Map.of("1", "2"), Map.of("1", 100), Map.of("1", "CHOICE"), null);
        ReflectionTestUtils.setField(exam, "id", 5L);
        ReflectionTestUtils.setField(exam, "version", 0L);
        return exam;
    }
}
