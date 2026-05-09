package com.lumie.exam.application.dto.response.statistics;

import java.util.List;

public record GoalSimulationResponse(
        Long studentId,
        int currentGrade,
        int targetGrade,
        int currentScore,
        int targetScore,
        int scoreDifference,
        boolean achievable,
        List<ImprovementScenario> scenarios
) {
    public record ImprovementScenario(
            String questionType,
            int currentCorrect,
            int totalQuestions,
            double currentAccuracy,
            int additionalCorrectNeeded,
            double targetAccuracy,
            int potentialScoreGain,
            String priority
    ) {}
}
