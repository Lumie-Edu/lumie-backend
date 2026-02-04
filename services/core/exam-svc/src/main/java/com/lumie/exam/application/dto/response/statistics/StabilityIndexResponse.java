package com.lumie.exam.application.dto.response.statistics;

import java.util.List;

public record StabilityIndexResponse(
        Long studentId,
        int examCount,
        double averageScore,
        double standardDeviation,
        double coefficientOfVariation,
        String stabilityLevel,
        List<ScoreHistory> scoreHistory
) {
    public record ScoreHistory(
            Long examId,
            String examName,
            int score,
            int grade,
            java.time.LocalDateTime examDate
    ) {}
}
