package com.lumie.billing.application.service;

import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
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
}
