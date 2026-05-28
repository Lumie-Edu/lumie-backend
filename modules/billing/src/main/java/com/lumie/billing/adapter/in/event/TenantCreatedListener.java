package com.lumie.billing.adapter.in.event;

import com.lumie.billing.application.service.SubscriptionCommandService;
import com.lumie.tenant.api.TenantCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link TenantCreatedEvent} after the tenant-creation commit
 * and provisions a Trial PRO subscription (30 days) for the new tenant
 * by delegating to {@link SubscriptionCommandService#provisionTrial}.
 *
 * <p>Adapter responsibility is event translation only — orchestration
 * (idempotency check, plan lookup, factory call) lives in the application
 * service per hexagonal direction (adapter → application → domain).
 *
 * <p>Uses Spring Modulith's {@link ApplicationModuleListener}: the event is
 * persisted to the {@code event_publication} outbox in the originating
 * transaction, then this method runs in its own transaction after commit.
 * Failed dispatches stay incomplete and are retried on application restart.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantCreatedListener {

    private final SubscriptionCommandService subscriptionCommandService;

    @ApplicationModuleListener
    public void onTenantCreated(TenantCreatedEvent event) {
        subscriptionCommandService.provisionTrial(event.tenantId(), event.tenantSlug());
    }
}
