package com.lumie.billing.application.port.out;

import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.vo.SubscriptionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SubscriptionPersistencePort {
    Subscription save(Subscription subscription);
    Optional<Subscription> findById(Long id);
    Optional<Subscription> findByTenantId(Long tenantId);
    Optional<Subscription> findByTenantSlug(String tenantSlug);
    Optional<Subscription> findActiveByTenantSlug(String tenantSlug);
    List<Subscription> findAllByStatus(SubscriptionStatus status);

    /**
     * Find ACTIVE, non-cancelled subscriptions whose {@code nextChargeAt} is on or before
     * {@code threshold}. Excludes subscriptions with {@code cancelledAt IS NOT NULL} so
     * cancel-grace-period subscriptions are not auto-charged (G-flow guard).
     */
    List<Subscription> findActiveWithNextChargeAtOrBefore(Instant threshold);

    /**
     * Find ACTIVE trial subscriptions whose {@code expiresAt} has passed.
     * Trials are identified by {@code billingKeyId IS NULL}. Uses
     * {@code idx_subscriptions_trial_expiry} (V37).
     */
    List<Subscription> findTrialsToExpire(Instant now);

    /**
     * Find subscriptions whose {@code pendingPlanId} is set and
     * {@code scheduledChangeAt} has passed. Uses
     * {@code idx_subscriptions_scheduled_change_at} (V37).
     */
    List<Subscription> findPendingDowngradesToApply(Instant now);

    /**
     * Find ACTIVE subscriptions with {@code cancelledAt IS NOT NULL} and
     * {@code nextChargeAt} on or before {@code now} (cancel-grace period ended).
     * Uses {@code idx_subscriptions_cancel_grace} (V37).
     */
    List<Subscription> findCancellationsToFinalize(Instant now);

    boolean existsByTenantId(Long tenantId);
    void delete(Subscription subscription);
}
