package com.lumie.billing.adapter.in.event;

import com.lumie.billing.application.port.in.CreateSubscriptionUseCase;
import com.lumie.tenant.api.TenantCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for {@link TenantCreatedEvent} after the tenant-creation commit
 * and provisions a FREE subscription for the new tenant.
 *
 * <p>Replaces the direct {@code tenant.TenantCommandService → billing.BillingService.createFreeSubscription}
 * call that created the tenant↔billing circular dependency
 * (see {@code temp/audits-cleanup/CIRCULAR_MODULE_DEPS_AUDIT.md}, Resolution A pair 3).
 *
 * <p>{@code AFTER_COMMIT} keeps the subscription side-effect outside the
 * tenant-creation transaction — matches the HARD_RULES_CROSS rule that
 * external HTTP calls (Paddle gateway) must run outside {@code @Transactional}
 * boundaries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantCreatedListener {

    private final CreateSubscriptionUseCase createSubscriptionUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTenantCreated(TenantCreatedEvent event) {
        try {
            createSubscriptionUseCase.createFreeSubscription(event.tenantId(), event.tenantSlug());
            log.info("FREE subscription provisioned for new tenant: {}", event.tenantSlug());
        } catch (Exception e) {
            log.error("Failed to provision FREE subscription for tenant {}: {}",
                    event.tenantSlug(), e.getMessage(), e);
        }
    }
}
