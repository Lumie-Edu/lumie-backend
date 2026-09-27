package com.lumie.exam.application.event;

import com.lumie.event.DomainEvent;

/**
 * Published (one per image) inside the {@code confirmBatchOmrGrading} transaction.
 * Persisted to the Spring Modulith {@code event_publication} outbox atomically
 * with the {@code OmrGradingJob} row, then forwarded to RabbitMQ after commit by
 * {@code JobRequestForwarder}. Survives a crash between commit and broker
 * receipt — incomplete publications are republished on restart.
 *
 * <p>Schema version 1.
 */
public record OmrGradingRequestedEvent(
        Long jobId,
        Long examId,
        String tenantSlug,
        int imageIndex,
        int totalImages
) implements DomainEvent {
}
