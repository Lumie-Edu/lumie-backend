package com.lumie.billing.adapter.in.event;

import com.lumie.billing.application.service.SubscriptionCommandService;
import com.lumie.tenant.api.TenantCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link TenantCreatedEvent} after the tenant-creation commit
 * and provisions a FREE subscription for the new tenant.
 *
 * <p>Replaces the direct {@code tenant.TenantCommandService → billing.BillingService.createFreeSubscription}
 * call that created the tenant↔billing circular dependency
 * (see {@code temp/audits-cleanup/CIRCULAR_MODULE_DEPS_AUDIT.md}, Resolution A pair 3).
 *
 * <p>Uses Spring Modulith's {@link ApplicationModuleListener}: the event is
 * persisted to the {@code event_publication} outbox in the originating
 * transaction, then this method runs in its own transaction after commit.
 * Failed dispatches stay incomplete and are retried on application restart —
 * also keeps the Paddle-gateway HTTP call outside the tenant-creation
 * transaction (HARD_RULES_CROSS).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantCreatedListener {

    private final SubscriptionCommandService subscriptionCommandService;

    @ApplicationModuleListener
    public void onTenantCreated(TenantCreatedEvent event) {
        try {
            subscriptionCommandService.createFreeSubscription(event.tenantId(), event.tenantSlug());
            log.info("FREE subscription provisioned for new tenant: {}", event.tenantSlug());
        } catch (Exception e) {
            log.error("Failed to provision FREE subscription for tenant {}: {}",
                    event.tenantSlug(), e.getMessage(), e);
        }
    }
}
