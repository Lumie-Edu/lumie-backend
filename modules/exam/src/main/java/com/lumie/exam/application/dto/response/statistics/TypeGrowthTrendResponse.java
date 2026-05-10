package com.lumie.exam.application.dto.response.statistics;

import java.util.List;

public record TypeGrowthTrendResponse(
        Long studentId,
        List<TypeTrend> trends
) {
    public record TypeTrend(
            String questionType,
            List<TrendPoint> trendPoints,
            double overallGrowthRate
    ) {}

    public record TrendPoint(
            Long examId,
            String examName,
            java.time.Instant examDate,
            int correctCount,
            int totalCount,
            double accuracy
    ) {}
}
