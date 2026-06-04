package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.vo.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SubscriptionJpaRepository extends JpaRepository<Subscription, Long> {

    /**
     * Eager fetch of {@code plan} and (LEFT JOIN FETCH for nullable)
     * {@code pendingPlan}. Used by per-row scheduler transition handlers
     * that mutate either association inside a fresh {@code REQUIRES_NEW}
     * session where lazy-loading would issue extra SELECTs.
     */
    @Query("SELECT s FROM Subscription s " +
           "JOIN FETCH s.plan " +
           "LEFT JOIN FETCH s.pendingPlan " +
           "WHERE s.id = :id")
    Optional<Subscription> findByIdWithPlanAndPendingPlan(@Param("id") Long id);

    Optional<Subscription> findByTenantId(Long tenantId);

    Optional<Subscription> findByTenantSlug(String tenantSlug);

    @Query("SELECT s FROM Subscription s WHERE s.tenantSlug = :tenantSlug " +
           "AND s.status = com.lumie.billing.domain.vo.SubscriptionStatus.ACTIVE")
    Optional<Subscription> findActiveByTenantSlug(@Param("tenantSlug") String tenantSlug);

    List<Subscription> findAllByStatus(SubscriptionStatus status);

    /**
     * Monthly charge candidates: ACTIVE, non-cancelled, nextChargeAt overdue.
     * Excludes subscriptions with cancelledAt IS NOT NULL (cancel-grace guard, G-flow).
     */
    @Query("SELECT s FROM Subscription s JOIN FETCH s.plan " +
           "WHERE s.status = com.lumie.billing.domain.vo.SubscriptionStatus.ACTIVE " +
           "AND s.cancelledAt IS NULL " +
           "AND s.nextChargeAt IS NOT NULL AND s.nextChargeAt <= :threshold")
    List<Subscription> findActiveWithNextChargeAtOrBefore(@Param("threshold") Instant threshold);

    /**
     * Trial expiry candidates: ACTIVE + no billing key + expiresAt passed.
     * Uses idx_subscriptions_trial_expiry (V37).
     */
    @Query("SELECT s FROM Subscription s JOIN FETCH s.plan " +
           "WHERE s.billingKeyId IS NULL " +
           "AND s.expiresAt IS NOT NULL " +
           "AND s.expiresAt <= :now " +
           "AND s.status = com.lumie.billing.domain.vo.SubscriptionStatus.ACTIVE")
    List<Subscription> findTrialsToExpire(@Param("now") Instant now);

    /**
     * Pending downgrade candidates: pendingPlan set + scheduledChangeAt passed.
     * JOIN FETCH pendingPlan to avoid N+1 on the association.
     * Uses idx_subscriptions_scheduled_change_at (V37).
     */
    @Query("SELECT s FROM Subscription s JOIN FETCH s.plan JOIN FETCH s.pendingPlan " +
           "WHERE s.scheduledChangeAt IS NOT NULL " +
           "AND s.scheduledChangeAt <= :now " +
           "AND s.status = com.lumie.billing.domain.vo.SubscriptionStatus.ACTIVE")
    List<Subscription> findPendingDowngradesToApply(@Param("now") Instant now);

    /**
     * Cancellation finalization candidates: cancelledAt set + nextChargeAt passed + ACTIVE.
     * Uses idx_subscriptions_cancel_grace (V37).
     */
    @Query("SELECT s FROM Subscription s JOIN FETCH s.plan " +
           "WHERE s.cancelledAt IS NOT NULL " +
           "AND s.nextChargeAt IS NOT NULL " +
           "AND s.nextChargeAt <= :now " +
           "AND s.status = com.lumie.billing.domain.vo.SubscriptionStatus.ACTIVE")
    List<Subscription> findCancellationsToFinalize(@Param("now") Instant now);

    boolean existsByTenantId(Long tenantId);
}
