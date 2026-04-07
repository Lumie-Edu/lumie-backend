package com.lumie.exam.application.dto.message;

import java.io.Serializable;

public record ReportGenerationMessage(
        Long jobId,
        Long examId,
        Long studentId,
        String tenantSlug,
        int reportIndex,
        int totalReports
) implements Serializable {
}
