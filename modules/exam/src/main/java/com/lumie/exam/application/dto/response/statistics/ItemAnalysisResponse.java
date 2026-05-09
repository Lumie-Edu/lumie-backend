package com.lumie.exam.application.dto.response.statistics;

import java.util.List;

public record ItemAnalysisResponse(
        Long examId,
        String examName,
        int participantCount,
        int sampleSize,
        boolean reliabilityWarning,
        List<ItemPoint> items
) {
    public record ItemPoint(
            int questionNumber,
            String questionType,
            double pValue,
            double discrimination,
            Quadrant quadrant
    ) {}

    public enum Quadrant {
        GOOD,
        TOO_EASY,
        TOO_HARD,
        DISCARD
    }
}
