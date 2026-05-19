package com.lumie.exam.test;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Shared exam test fixtures — eliminates the {@code buildExam()} /
 * {@code buildGradedExam()} copy-paste that existed across four exam service tests.
 *
 * <p>Canonical 5-question ABSOLUTE / NINE_GRADE exam with 20 points per question.
 * Overloads let callers vary question count. All questions use {@code "단답"} type,
 * correct answers {@code "1"..N}, scores 20 each.
 */
public final class ExamFixtures {

    private ExamFixtures() {
    }

    /**
     * Returns a 5-question graded exam — the most common fixture in exam tests.
     * Equivalent to the former {@code buildGradedExam()} in ItemAnalysisQueryServiceTest
     * and ClassComparisonQueryServiceTest.
     */
    public static Exam aGradedExam() {
        return aGradedExam(5);
    }

    /**
     * Returns a graded exam with {@code qCount} questions (1-indexed answers).
     * Equivalent to the former {@code buildExam()} variants in OmrGradingJobProcessorTest
     * and ResultCommandServiceTest (which used 3 or 5 questions).
     */
    public static Exam aGradedExam(int qCount) {
        Map<String, String> correctAnswers = new LinkedHashMap<>();
        Map<String, Integer> questionScores = new LinkedHashMap<>();
        Map<String, String> questionTypes = new LinkedHashMap<>();
        for (int i = 1; i <= qCount; i++) {
            String key = String.valueOf(i);
            correctAnswers.put(key, key);
            questionScores.put(key, 20);
            questionTypes.put(key, "단답");
        }
        return Exam.create(
                "테스트 시험",
                ExamCategory.GRADED,
                GradingType.ABSOLUTE,
                GradeScale.NINE_GRADE,
                qCount,
                correctAnswers,
                questionScores,
                questionTypes,
                null
        );
    }

    /**
     * Creates an {@link ExamResult} for {@code studentId} with the given {@code totalScore},
     * adding one {@link QuestionResult} per exam question where each answer is correct
     * (selected == correct answer). Useful for tests that need question-level data.
     *
     * @param exam      the exam (typically from {@link #aGradedExam()})
     * @param studentId the student; may be {@code null} for unregistered students
     * @param score     total score to record
     * @param corrects  per-question correctness flags; list size must equal exam's totalQuestions
     */
    public static ExamResult anExamResult(Exam exam, Long studentId, int score, java.util.List<Boolean> corrects) {
        ExamResult result = ExamResult.create(exam, studentId, score);
        for (int i = 0; i < corrects.size(); i++) {
            String correctAnswer = String.valueOf(i + 1);
            String selected = corrects.get(i) ? correctAnswer : "0";
            result.addQuestionResult(QuestionResult.create(i + 1, selected, correctAnswer, 20));
        }
        return result;
    }

    /**
     * Convenience: creates a fully-correct result (all answers correct).
     */
    public static ExamResult anExamResult(Exam exam, Long studentId, int score) {
        int qCount = exam.getTotalQuestions();
        java.util.List<Boolean> allCorrect = IntStream.range(0, qCount)
                .mapToObj(i -> true)
                .collect(Collectors.toList());
        return anExamResult(exam, studentId, score, allCorrect);
    }
}
