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
     * Atomically attach a billing key and upgrade-or-reactivate the
     * subscription onto the given paid plan. Used by
     * {@code BillingSubscribeService.subscribe} so the "switch to paid plan
     * with this card" mutation is one DB tx — the FE never observes
     * "new card attached but still on the old plan / still CANCELLED" mid-flow.
     *
     * <p>Delegates to {@link Subscription#upgradeOrReactivate} which absorbs
     * the state-machine adapter: ACTIVE / PAUSED / PAST_DUE / CANCELLED /
     * EXPIRED all converge to "ACTIVE on newPlan". Only PENDING is rejected
     * (a prior subscribe is in-flight).
     */
    @Transactional
    public Subscription attachBillingKeyAndUpgrade(String tenantSlug, Long billingKeyId,
                                                   Plan newPlan, java.time.Instant newExpiresAt) {
        Subscription subscription = subscriptionPersistencePort.findByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));
        subscription.attachBillingKey(billingKeyId);
        subscription.upgradeOrReactivate(newPlan, newExpiresAt);
        return subscriptionPersistencePort.save(subscription);
    }
}
