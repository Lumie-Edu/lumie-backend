package com.lumie.exam.application.dto.response.statistics;

import java.util.List;

public record NormalizedScoreResponse(
        Long studentId,
        List<NormalizedExamScore> normalizedScores
) {
    public record NormalizedExamScore(
            Long examId,
            String examName,
            java.time.LocalDateTime examDate,
            int rawScore,
            double examMean,
            double examStdDev,
            double zScore,
            double normalizedScore
    ) {}
}
