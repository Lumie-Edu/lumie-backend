package com.lumie.exam.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.vo.ExamSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExamRegradeCalculator {

    private final GradeCalculator gradeCalculator;

    public Outcome calculate(Exam exam, Map<Integer, String> selectedAnswers, Map<String, String> answerKey) {
        if (exam.getSource() != ExamSource.EXAM_MANAGEMENT) {
            throw invalidData("Only exam-management exams can be regraded.");
        }
        if (exam.getTotalQuestions() == null || exam.getTotalQuestions() < 1
                || exam.getQuestionScores() == null
                || selectedAnswers == null || selectedAnswers.size() != exam.getTotalQuestions()
                || answerKey == null || answerKey.size() != exam.getTotalQuestions()) {
            throw invalidData("Question answers are incomplete.");
        }

        List<QuestionOutcome> questionOutcomes = new ArrayList<>(exam.getTotalQuestions());
        int totalScore = 0;
        for (int questionNumber = 1; questionNumber <= exam.getTotalQuestions(); questionNumber++) {
            String questionKey = String.valueOf(questionNumber);
            if (!selectedAnswers.containsKey(questionNumber)) {
                throw invalidData("Question answers are incomplete.");
            }
            String correctAnswer = answerKey.get(questionKey);
            Integer maxScore = exam.getQuestionScores().get(questionKey);
            if (correctAnswer == null || correctAnswer.isBlank() || maxScore == null || maxScore < 0) {
                throw invalidData("Exam answer key or question score is incomplete.");
            }

            String selectedAnswer = selectedAnswers.get(questionNumber);
            int earnedScore = selectedAnswer != null && selectedAnswer.equals(correctAnswer) ? maxScore : 0;
            questionOutcomes.add(new QuestionOutcome(
                    questionNumber, selectedAnswer, correctAnswer, maxScore, earnedScore));
            totalScore += earnedScore;
        }

        ExamResult projectedResult = ExamResult.create(exam, null, totalScore);
        gradeCalculator.calculateAndAssignGradeIfAbsolute(exam, projectedResult);
        return new Outcome(totalScore, projectedResult.getGrade(), List.copyOf(questionOutcomes));
    }

    private static BusinessException invalidData(String message) {
        return new BusinessException(ExamErrorCode.INVALID_ANSWER_DATA, message);
    }

    public record Outcome(int totalScore, Integer storedGrade, List<QuestionOutcome> questions) {}

    public record QuestionOutcome(
            int questionNumber,
            String selectedAnswer,
            String correctAnswer,
            int maxScore,
            int earnedScore
    ) {}
}
