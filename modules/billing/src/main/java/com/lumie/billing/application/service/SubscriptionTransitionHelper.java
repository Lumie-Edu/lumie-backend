package com.lumie.billing.application.service;

import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-row transactional helper for scheduled subscription state transitions (Phase E D6).
 *
 * <p>Each public method runs in its own {@code REQUIRES_NEW} transaction so that a
 * single-row failure does not roll back sibling rows in the same scheduler tick.
 * Extracted as a separate Spring bean to honour the Spring AOP proxy contract —
 * self-invocation from {@link com.lumie.billing.adapter.in.scheduling.SubscriptionScheduledChangeScheduler}
 * would bypass the proxy and lose the transaction boundary.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionTransitionHelper {

    private final SubscriptionPersistencePort subscriptionPersistencePort;

    /**
     * Expire a single trial subscription (B-flow).
     * Calls {@link Subscription#expireTrial()} which is idempotent for already-EXPIRED rows.
     *
     * @throws BillingException if the subscription is not found or has a billing key
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireOneTrial(Long subscriptionId) {
        Subscription sub = subscriptionPersistencePort.findById(subscriptionId)
                .orElseThrow(() -> new BillingException(BillingErrorCode.SUBSCRIPTION_NOT_FOUND,
                        "Subscription not found for trial expiry: " + subscriptionId));
        sub.expireTrial();
        subscriptionPersistencePort.save(sub);
    }

    /**
     * Apply a pending plan change (downgrade) to a single subscription (F-flow).
     * Calls {@link Subscription#applyPendingPlanChange()} which is idempotent.
     *
     * @throws BillingException if the subscription is not found
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyOnePendingChange(Long subscriptionId) {
        Subscription sub = subscriptionPersistencePort.findById(subscriptionId)
                .orElseThrow(() -> new BillingException(BillingErrorCode.SUBSCRIPTION_NOT_FOUND,
                        "Subscription not found for pending plan change: " + subscriptionId));
        sub.applyPendingPlanChange();
        subscriptionPersistencePort.save(sub);
    }

    /**
     * Finalize the cancellation of a single subscription (G-flow).
     * Calls {@link Subscription#finalizeCancellation()} which is idempotent for already-CANCELLED rows.
     *
     * @throws BillingException if the subscription is not found
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeOneCancellation(Long subscriptionId) {
        Subscription sub = subscriptionPersistencePort.findById(subscriptionId)
                .orElseThrow(() -> new BillingException(BillingErrorCode.SUBSCRIPTION_NOT_FOUND,
                        "Subscription not found for cancellation finalization: " + subscriptionId));
        sub.finalizeCancellation();
        subscriptionPersistencePort.save(sub);
    }
}
