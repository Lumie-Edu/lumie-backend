package com.lumie.exam.application.dto.response.statistics;

import java.util.List;

public record StudentRankResponse(
        Long studentId,
        Long examId,
        String examName,
        int score,
        int rank,
        int totalParticipants,
        double percentile,
        Integer grade,
        List<TypePercentile> typePercentiles
) {
    public record TypePercentile(
            String questionType,
            int correctCount,
            int totalCount,
            double accuracy,
            double percentile
    ) {}
}
