package com.lumie.exam.application.dto.response.statistics;

import java.util.List;

public record DashboardStatisticsResponse(
        int totalExams,
        int totalResults,
        double overallAverageScore,
        List<TypeAccuracy> typeAccuracyList,
        List<TopIncorrectQuestion> topIncorrectQuestions
) {
    public record TypeAccuracy(
            String questionType,
            int totalQuestions,
            int correctCount,
            double accuracyRate
    ) {}

    public record TopIncorrectQuestion(
            Long examId,
            String examName,
            int questionNumber,
            String questionType,
            String correctAnswer,
            int totalAttempts,
            int incorrectCount,
            double incorrectRate,
            String topWrongChoice,
            double topWrongChoiceRate
    ) {}
}
