package com.lumie.exam.application.dto.request;

import java.util.List;

public record OmrGradingCallbackRequest(
        Long jobId,
        Long examId,
        String tenantSlug,
        String imageKey,
        int imageIndex,
        int totalImages,
        boolean success,
        String error,
        String phoneNumber,
        int totalScore,
        int grade,
        List<QuestionResultItem> results
) {
    public record QuestionResultItem(
            int questionNumber,
            String studentAnswer,
            String correctAnswer,
            int score,
            int earnedScore,
            String questionType
    ) {
    }
}
