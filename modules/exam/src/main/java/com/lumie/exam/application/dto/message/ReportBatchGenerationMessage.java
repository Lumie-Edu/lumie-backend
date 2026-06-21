package com.lumie.exam.application.dto.message;

import java.io.Serializable;
import java.util.List;

public record ReportBatchGenerationMessage(
        Long jobId,
        Long examId,
        List<Long> studentIds,
        String tenantSlug,
        int startIndex,
        int totalReports,
        int schemaVersion
) implements Serializable {
}
