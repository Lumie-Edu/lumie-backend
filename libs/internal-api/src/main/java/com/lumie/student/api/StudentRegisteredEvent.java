package com.lumie.student.api;

import com.lumie.event.DomainEvent;

/**
 * Domain event published after a Student is registered (single or bulk path).
 *
 * <p>Other modules (notably exam) listen with
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} to backfill
 * cross-module work that previously required a synchronous call back into
 * student. Replaces the direct {@code student → exam.linkUnregistered}
 * call that created the student↔exam circular dependency.
 *
 * <p>Schema version 1 (the DomainEvent default).
 *
 * @param studentId persisted student id
 * @param phone phone number recorded on the student (may be null/blank)
 * @param tenantSlug tenant slug snapshot for listeners that run after the
 *                   request thread is recycled — allows them to
 *                   {@link com.lumie.common.tenant.TenantContextHolder#withinContext}
 *                   restore.
 * @param tenantId tenant id snapshot for RLS-scoped listeners that run after the
 *                 request thread is recycled.
 */
public record StudentRegisteredEvent(
        Long studentId,
        String phone,
        String tenantSlug,
        Long tenantId
) implements DomainEvent {
}
