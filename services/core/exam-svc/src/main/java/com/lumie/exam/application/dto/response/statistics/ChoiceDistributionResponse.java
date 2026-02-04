package com.lumie.exam.application.dto.response.statistics;

import java.util.List;
import java.util.Map;

public record ChoiceDistributionResponse(
        Long examId,
        String examName,
        int totalParticipants,
        List<QuestionChoiceDistribution> questions
) {
    public record QuestionChoiceDistribution(
            int questionNumber,
            String questionType,
            String correctAnswer,
            int correctCount,
            double correctRate,
            Map<String, ChoiceStats> choiceDistribution,
            List<AttractiveDistractor> attractiveDistractors
    ) {}

    public record ChoiceStats(
            int count,
            double percentage,
            boolean isCorrect
    ) {}

    public record AttractiveDistractor(
            String choice,
            int count,
            double percentage
    ) {}
}
