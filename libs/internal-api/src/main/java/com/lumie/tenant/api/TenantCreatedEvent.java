package com.lumie.tenant.api;

/**
 * Domain event published after a Tenant is created and activated.
 *
 * <p>Other modules (notably billing) listen with
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} to perform
 * cross-module work that previously required a synchronous call back into
 * tenant. Replaces the direct {@code tenant → billing.createFreeSubscription}
 * call that created the tenant↔billing circular dependency
 * (see {@code temp/audits-cleanup/CIRCULAR_MODULE_DEPS_AUDIT.md}, Resolution A pair 3).
 */
public record TenantCreatedEvent(
        Long tenantId,
        String tenantSlug
) {
}
