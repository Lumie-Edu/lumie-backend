package com.lumie.exam.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-process internal API exposed by exam-svc and consumed by other services.
 */
public interface ExamService {

    /**
     * Links previously unregistered exam results (student_id IS NULL, matching phone_number,
     * not marked as deleted-student history) to a newly registered student when the
     * phone resolves to exactly one clear unregistered candidate for that exam. Duplicate
     * candidates in the same exam are skipped as ambiguous. Runs within the current tenant scope.
     *
     * @param phoneNumber the phone number recorded on unregistered exam results
     * @param studentId   the student id to backfill
     * @return number of exam_results rows updated
     */
    int linkUnregisteredResultsByPhone(String phoneNumber, Long studentId);

    Long createExamForAssignment(String tenantSlug, CreateExamData request);

    Optional<ExamAssignmentData> getExamForAssignment(String tenantSlug, Long examId);

    ManualGradeResult gradeManualAnswers(String tenantSlug, Long examId, Long studentId, Map<String, String> answers);

    List<QuestionGradeData> getQuestionResultsForAssignment(String tenantSlug, Long examResultId);

    record ExamAssignmentData(
            Long id,
            String name,
            String category,
            String gradingType,
            String gradeScale,
            int totalQuestions,
            int totalPossibleScore,
            Integer passScore,
            List<QuestionMetadata> questions
    ) {
    }

    record CreateExamData(
            String name,
            String category,
            String gradingType,
            String gradeScale,
            Integer totalQuestions,
            Map<String, String> correctAnswers,
            Map<String, Integer> questionScores,
            Map<String, String> questionTypes,
            Integer passScore
    ) {
    }

    record QuestionMetadata(
            Integer questionNumber,
            Integer score,
            String questionType
    ) {
    }

    record ManualGradeResult(
            Long examResultId,
            Long examId,
            Long studentId,
            Integer totalScore,
            Integer totalPossibleScore,
            Integer grade,
            Boolean passed,
            List<QuestionGradeData> questionResults
    ) {
    }

    record QuestionGradeData(
            Long questionResultId,
            Integer questionNumber,
            String selectedChoice,
            Boolean correct,
            Integer score,
            Integer earnedScore,
            String correctAnswer,
            String questionType
    ) {
    }
}
