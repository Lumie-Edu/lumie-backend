package com.lumie.exam.application.dto.response.statistics;

import io.swagger.v3.oas.annotations.media.Schema;
import com.lumie.exam.domain.vo.ExamCategory;

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
            int totalPossibleScore,
            @Schema(nullable = true) Integer grade,
            @Schema(nullable = true) Boolean isPassed,
            ExamCategory examCategory,
            java.time.Instant examDate
    ) {}
}
