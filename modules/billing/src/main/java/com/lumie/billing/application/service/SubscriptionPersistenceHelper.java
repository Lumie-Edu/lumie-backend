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
     * Atomically attach a billing key and change the plan for an existing
     * subscription. Used by {@code BillingSubscribeService.subscribe} so the
     * "switch to paid plan with this card" mutation is one DB tx — the FE
     * never observes "new card attached but still on FREE" mid-flow.
     */
    @Transactional
    public Subscription attachBillingKeyAndChangePlan(String tenantSlug, Long billingKeyId, Plan newPlan) {
        Subscription subscription = subscriptionPersistencePort.findByTenantSlug(tenantSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", tenantSlug));
        subscription.attachBillingKey(billingKeyId);
        subscription.changePlan(newPlan);
        return subscriptionPersistencePort.save(subscription);
    }
}
