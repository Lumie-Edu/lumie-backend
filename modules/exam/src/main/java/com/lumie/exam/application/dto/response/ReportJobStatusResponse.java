package com.lumie.exam.application.dto.response;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReportJobStatusResponse(
        Long jobId,
        String status,
        int totalReports,
        int processedReports,
        int successCount,
        int failCount,
        @Schema(nullable = true, description = "Temporary object key after ZIP generation") String zipFileKey,
        @Schema(nullable = true, description = "UTC time after which the report ZIP is unavailable") Instant expiresAt
) {
}
