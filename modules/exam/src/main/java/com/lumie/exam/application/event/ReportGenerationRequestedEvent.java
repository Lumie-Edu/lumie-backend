package com.lumie.exam.application.event;

import com.lumie.event.DomainEvent;

/**
 * Published (one per report) inside the {@code createBatchJob} transaction.
 * Persisted to the Spring Modulith {@code event_publication} outbox atomically
 * with the {@code ReportGenerationJob} row, then forwarded to RabbitMQ after
 * commit by {@code JobRequestForwarder}. Survives a crash between commit and
 * broker receipt — incomplete publications are republished on restart.
 *
 * <p>Schema version 1.
 */
public record ReportGenerationRequestedEvent(
        Long jobId,
        Long examId,
        Long studentId,
        String tenantSlug,
        int reportIndex,
        int totalReports
) implements DomainEvent {
}
