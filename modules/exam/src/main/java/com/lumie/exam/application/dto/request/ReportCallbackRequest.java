package com.lumie.exam.application.dto.request;

public record ReportCallbackRequest(
        Long jobId,
        Long examId,
        Long studentId,
        String tenantSlug,
        int reportIndex,
        int totalReports,
        boolean success,
        String error,
        String fileKey,
        String reportBytes
) {
}
