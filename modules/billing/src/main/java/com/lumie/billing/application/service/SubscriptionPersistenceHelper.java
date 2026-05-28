package com.lumie.billing.application.service;

import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.domain.entity.Plan;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional helper for subscription persistence.
 * Extracted so callers without a class-level @Transactional can commit before
 * making external PG calls (HARD_RULES: no external HTTP inside @Transactional).
 */
@Service
@RequiredArgsConstructor
public class SubscriptionPersistenceHelper {

    private final SubscriptionPersistencePort subscriptionPersistencePort;

    @Transactional
    public Subscription save(Subscription subscription) {
        return subscriptionPersistencePort.save(subscription);
    }

    @Transactional
    public void recordChargeAttemptFailed(Long subscriptionId) {
        Subscription subscription = subscriptionPersistencePort.findById(subscriptionId)
                .orElseThrow(() -> new BillingException(BillingErrorCode.SUBSCRIPTION_NOT_FOUND,
                        "Subscription not found: " + subscriptionId));
        subscription.chargeAttemptFailed();
        subscriptionPersistencePort.save(subscription);
    }

    /**
     * Atomically attach a billing key and subscribe the tenant onto the given paid plan.
     * Used by {@code BillingSubscribeService.subscribe} so the "switch to paid plan with
     * this card" mutation is one DB tx — the FE never observes "new card attached but
     * still on the old plan / still CANCELLED" mid-flow.
     *
     * <p>Delegates to {@link Subscription#subscribe(Long, Plan, java.time.Instant)} which
     * absorbs the state-machine: ACTIVE / PAUSED / PAST_DUE / CANCELLED / EXPIRED all
     * converge to "ACTIVE on newPlan".
     */
    @Transactional
    public Subscription attachBillingKeyAndSubscribe(String tenantSlug, Long billingKeyId,
                                                     Plan newPlan, java.time.Instant nextChargeAt) {
        if (nextChargeAt == null) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "nextChargeAt is required — caller (BillingSubscribeService) must compute "
                            + "the next billing cycle before invoking attachBillingKeyAndSubscribe");
        }
        Subscription subscription = subscriptionPersistencePort.findByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));
        subscription.subscribe(billingKeyId, newPlan, nextChargeAt);
        return subscriptionPersistencePort.save(subscription);
    }
}
