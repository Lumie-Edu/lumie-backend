package com.lumie.tenant.api;

import com.lumie.event.DomainEvent;

/**
 * Domain event published after a Tenant is created and activated.
 *
 * <p>Other modules (notably billing) listen with
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} to perform
 * cross-module work that previously required a synchronous call back into
 * tenant. Replaces the direct {@code tenant → billing.createFreeSubscription}
 * call that created the tenant↔billing circular dependency.
 *
 * <p>Schema version 1.
 */
public record TenantCreatedEvent(
        Long tenantId,
        String tenantSlug
) implements DomainEvent {
}
