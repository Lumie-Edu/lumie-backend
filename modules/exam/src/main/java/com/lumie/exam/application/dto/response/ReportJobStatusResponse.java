package com.lumie.exam.application.dto.response;

public record ReportJobStatusResponse(
        Long jobId,
        String status,
        int totalReports,
        int processedReports,
        int successCount,
        int failCount,
        String zipFileKey
) {
}
