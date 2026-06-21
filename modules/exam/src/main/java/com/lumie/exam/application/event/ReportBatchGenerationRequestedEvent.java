package com.lumie.exam.application.event;

import com.lumie.event.DomainEvent;

import java.util.List;

/**
 * Published once per report chunk. Schema version 2 lets report-svc fetch the
 * shared exam/report data once for the chunk and emit per-student callbacks.
 */
public record ReportBatchGenerationRequestedEvent(
        Long jobId,
        Long examId,
        List<Long> studentIds,
        String tenantSlug,
        int startIndex,
        int totalReports
) implements DomainEvent {
}
