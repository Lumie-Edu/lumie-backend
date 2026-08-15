package com.lumie.exam.application.dto.response;

import java.time.Instant;

/**
 * Compact OMR completion summary for the admin dashboard.
 */
public record RecentCompletedExamResponse(
        Long examId,
        String examName,
        Instant completedAt,
        long participantCount,
        double averageScore
) {
}
