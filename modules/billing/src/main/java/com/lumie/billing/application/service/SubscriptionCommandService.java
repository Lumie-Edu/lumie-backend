package com.lumie.billing.application.service;

import com.lumie.billing.adapter.out.config.BillingProperties;
import com.lumie.billing.application.dto.request.ChangeSubscriptionRequest;
import com.lumie.billing.application.dto.request.CreateSubscriptionRequest;
import com.lumie.billing.application.dto.response.InvoiceResponse;
import com.lumie.billing.application.dto.response.SubscriptionResponse;
import com.lumie.billing.application.port.out.InvoicePersistencePort;
import com.lumie.billing.application.port.out.PlanPersistencePort;
import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.application.service.SubscriptionChargeExecutor.ChargeOutcome;
import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.entity.Plan;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.InvoiceStatus;
import com.lumie.billing.domain.vo.SuspensionReason;
import com.lumie.common.exception.DuplicateResourceException;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionCommandService {

    private final BillingProperties billingProperties;

    private final SubscriptionPersistencePort subscriptionPersistencePort;
    private final PlanPersistencePort planPersistencePort;
    private final InvoicePersistencePort invoicePersistencePort;
    private final SubscriptionChargeExecutor chargeExecutor;

    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        log.info("Creating subscription for tenant: {} with plan: {}",
                request.tenantSlug(), request.planId());

        if (subscriptionPersistencePort.existsByTenantId(request.tenantId())) {
            throw new DuplicateResourceException("Subscription", "tenantId:" + request.tenantId());
        }

        Plan plan = planPersistencePort.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", request.planId()));

        Instant expiresAt = plan.isFree() ? null :
                Instant.now().atZone(ZoneOffset.UTC).plusMonths(request.billingCycleMonths()).toInstant();

        Subscription subscription = Subscription.create(
                request.tenantId(), request.tenantSlug(), plan, expiresAt);

        Subscription saved = subscriptionPersistencePort.save(subscription);
        log.info("Subscription created: id={}, tenantSlug={}, planId={}",
                saved.getId(), saved.getTenantSlug(), saved.getPlanId());

        return SubscriptionResponse.from(saved);
    }

    public SubscriptionResponse createFreeSubscription(Long tenantId, String tenantSlug) {
        log.info("Creating free subscription for tenant: {}", tenantSlug);

        if (subscriptionPersistencePort.existsByTenantId(tenantId)) {
            log.warn("Subscription already exists for tenant: {}", tenantSlug);
            return subscriptionPersistencePort.findByTenantId(tenantId)
                    .map(SubscriptionResponse::from)
                    .orElseThrow(() -> new ResourceNotFoundException("Subscription", "tenantId:" + tenantId));
        }

        String freePlanId = billingProperties.subscription().freePlanId();
        Plan freePlan = planPersistencePort.findById(freePlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", freePlanId));

        Subscription subscription = Subscription.createFree(tenantId, tenantSlug, freePlan);
        Subscription saved = subscriptionPersistencePort.save(subscription);

        log.info("Free subscription created: id={}, tenantSlug={}", saved.getId(), saved.getTenantSlug());
        return SubscriptionResponse.from(saved);
    }

    public SubscriptionResponse changePlan(String tenantSlug, ChangeSubscriptionRequest request) {
        log.info("Changing plan for tenant: {} to plan: {}", tenantSlug, request.newPlanId());

        Subscription subscription = subscriptionPersistencePort.findActiveByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));

        String previousPlanId = subscription.getPlanId();

        Plan newPlan = planPersistencePort.findById(request.newPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", request.newPlanId()));

        if (newPlan.getMonthlyPrice().isLessThan(subscription.getPlan().getMonthlyPrice())) {
            // Downgrade: defer to current period end (next_charge_at). A paid ACTIVE
            // subscription always has next_charge_at set (BillingSubscribeService stamps
            // it on subscribe); if absent, refuse rather than silently apply immediately
            // — the absence signals a corrupted state caller must investigate.
            Instant effectiveAt = subscription.getNextChargeAt();
            if (effectiveAt == null) {
                throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                        "Cannot schedule downgrade: subscription has no next_charge_at. "
                                + "Tenant: " + tenantSlug);
            }
            subscription.scheduleDowngrade(newPlan, effectiveAt);
        } else {
            // Upgrade or same-price: immediate.
            subscription.changePlan(newPlan);
        }

        Subscription saved = subscriptionPersistencePort.save(subscription);
        log.info("Plan changed for tenant: {} from {} to {} (downgrade={})",
                tenantSlug, previousPlanId, request.newPlanId(), saved.isDowngradeScheduled());
        return SubscriptionResponse.from(saved);
    }

    public SubscriptionResponse undoCancellation(String tenantSlug) {
        log.info("Undoing scheduled cancellation for tenant: {}", tenantSlug);

        Subscription subscription = subscriptionPersistencePort.findActiveByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));

        subscription.undoScheduledCancellation();
        Subscription saved = subscriptionPersistencePort.save(subscription);
        log.info("Cancellation undone for tenant: {}", tenantSlug);
        return SubscriptionResponse.from(saved);
    }

    public void cancelSubscription(String tenantSlug) {
        log.info("Scheduling cancellation for tenant: {}", tenantSlug);

        Subscription subscription = subscriptionPersistencePort.findByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));

        subscription.scheduleCancellation();
        subscriptionPersistencePort.save(subscription);
        log.info("Cancellation scheduled for tenant: {}", tenantSlug);
    }

    public void pauseSubscription(String tenantSlug, SuspensionReason reason) {
        log.info("Pausing subscription for tenant: {}, reason={}", tenantSlug, reason);

        Subscription subscription = subscriptionPersistencePort.findByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));

        subscription.pause(reason);
        subscriptionPersistencePort.save(subscription);
        log.info("Subscription paused for tenant: {}", tenantSlug);
    }

    public void resumeSubscription(String tenantSlug) {
        log.info("Resuming subscription for tenant: {}", tenantSlug);

        Subscription subscription = subscriptionPersistencePort.findByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));

        subscription.resume();
        subscriptionPersistencePort.save(subscription);
        log.info("Subscription resumed for tenant: {}", tenantSlug);
    }

    /**
     * Trigger the first paid charge on a newly created subscription once a billing key is attached.
     *
     * <p>Idempotency: if a PAID invoice already exists for this subscription, the existing invoice is
     * returned without contacting the PG. Request-level dedup (same Idempotency-Key) is handled by
     * the controller's {@code IdempotencyService.executeOnce}.
     *
     * <p>Runs without a surrounding transaction so the PG call in {@link SubscriptionChargeExecutor}
     * is outside JDBC scope (HARD_RULES).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public InvoiceResponse chargeFirstInvoice(String tenantSlug) {
        log.info("First-charge requested for tenant: {}", tenantSlug);

        Subscription subscription = subscriptionPersistencePort.findActiveByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));

        if (subscription.getPlan().isFree()) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "Free plan does not require a charge: " + tenantSlug);
        }
        if (subscription.getBillingKeyId() == null) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "Subscription has no billing key attached: " + tenantSlug);
        }

        List<Invoice> existing = invoicePersistencePort.findBySubscriptionId(subscription.getId());
        Invoice alreadyPaid = existing.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PAID)
                .max(Comparator.comparing(Invoice::getCreatedAt))
                .orElse(null);
        if (alreadyPaid != null) {
            log.info("First charge already settled — returning existing invoice: subscriptionId={}, invoiceId={}",
                    subscription.getId(), alreadyPaid.getId());
            return InvoiceResponse.from(alreadyPaid);
        }

        ChargeOutcome outcome = chargeExecutor.charge(subscription);
        return switch (outcome.status()) {
            case PAID -> InvoiceResponse.from(outcome.invoice());
            case FAILED -> throw new BillingException(BillingErrorCode.PAYMENT_FAILED,
                    outcome.reason() != null ? outcome.reason() : "PG charge failed");
            case SKIPPED -> throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "Charge skipped: " + (outcome.reason() != null ? outcome.reason() : "unknown"));
        };
    }
}
