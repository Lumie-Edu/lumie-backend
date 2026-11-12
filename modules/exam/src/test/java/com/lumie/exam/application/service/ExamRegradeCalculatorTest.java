package com.lumie.exam.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import com.lumie.exam.domain.vo.ExamSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExamRegradeCalculatorTest {

    private final ExamRegradeCalculator calculator = new ExamRegradeCalculator(new GradeCalculator());

    @Test
    void recalculatesQuestionPointsAndTotalFromSelectedAnswersAndProposedKey() {
        Exam exam = exam();

        var outcome = calculator.calculate(exam, Map.of(1, "4", 2, "1"),
                Map.of("1", "4", "2", "2"));

        assertThat(outcome.totalScore()).isEqualTo(2);
        assertThat(outcome.storedGrade()).isEqualTo(9);
        assertThat(outcome.questions())
                .extracting(ExamRegradeCalculator.QuestionOutcome::earnedScore)
                .containsExactly(2, 0);
        assertThat(outcome.questions())
                .extracting(ExamRegradeCalculator.QuestionOutcome::correctAnswer)
                .containsExactly("4", "2");
    }

    @Test
    void recalculatesWhenPreviouslyCorrectAnswerBecomesWrong() {
        var outcome = calculator.calculate(exam(), Map.of(1, "2", 2, "2"),
                Map.of("1", "4", "2", "2"));

        assertThat(outcome.totalScore()).isEqualTo(98);
        assertThat(outcome.questions().getFirst().earnedScore()).isZero();
        assertThat(outcome.questions().get(1).earnedScore()).isEqualTo(98);
    }

    @Test
    void rejectsMissingQuestionAnswersInsteadOfReturningPartialScore() {
        assertThatThrownBy(() -> calculator.calculate(exam(), Map.of(1, "4"), Map.of("1", "4", "2", "2")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsAssignmentOwnedExams() {
        Exam assignment = Exam.createForAssignment("Assignment", ExamCategory.GRADED, GradingType.ABSOLUTE,
                GradeScale.NINE_GRADE, 2, Map.of("1", "1", "2", "2"), Map.of("1", 2, "2", 98),
                Map.of("1", "CHOICE", "2", "CHOICE"), null);

        assertThat(assignment.getSource()).isEqualTo(ExamSource.ASSIGNMENT);
        assertThatThrownBy(() -> calculator.calculate(assignment, Map.of(1, "1", 2, "2"),
                Map.of("1", "2", "2", "2")))
                .isInstanceOf(BusinessException.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("incompleteAnswerCases")
    void rejectsIncompleteSavedQuestionData(String caseName, Map<Integer, String> selected,
                                          Map<String, String> answerKey) {
        assertThatThrownBy(() -> calculator.calculate(exam(), selected, answerKey))
                .isInstanceOf(BusinessException.class);
    }

    private static Stream<Arguments> incompleteAnswerCases() {
        return Stream.of(
                Arguments.of("null selected answers", null, Map.of("1", "2", "2", "2")),
                Arguments.of("null answer key", Map.of(1, "2", 2, "2"), null),
                Arguments.of("missing answer key", Map.of(1, "2", 2, "2"), Map.of("1", "2")),
                Arguments.of("wrong question number", Map.of(1, "2", 3, "2"), Map.of("1", "2", "2", "2")),
                Arguments.of("blank answer key", Map.of(1, "2", 2, "2"), Map.of("1", " ", "2", "2"))
        );
    }

    @Test
    void rejectsMissingScoreOrQuestionCountInPersistedExam() {
        Exam missingScore = exam();
        ReflectionTestUtils.setField(missingScore, "questionScores", Map.of("1", 2));
        assertThatThrownBy(() -> calculator.calculate(missingScore, Map.of(1, "2", 2, "2"),
                Map.of("1", "2", "2", "2"))).isInstanceOf(BusinessException.class);

        Exam missingCount = exam();
        ReflectionTestUtils.setField(missingCount, "totalQuestions", null);
        assertThatThrownBy(() -> calculator.calculate(missingCount, Map.of(1, "2", 2, "2"),
                Map.of("1", "2", "2", "2"))).isInstanceOf(BusinessException.class);
    }

    private static Exam exam() {
        return Exam.create("Regrade", ExamCategory.GRADED, GradingType.ABSOLUTE, GradeScale.NINE_GRADE,
                2, Map.of("1", "2", "2", "2"), Map.of("1", 2, "2", 98),
                Map.of("1", "CHOICE", "2", "CHOICE"), null);
    }
}
