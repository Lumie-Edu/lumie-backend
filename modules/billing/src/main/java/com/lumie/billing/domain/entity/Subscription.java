package com.lumie.billing.domain.entity;

import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.SubscriptionStatus;
import com.lumie.billing.domain.vo.SuspensionReason;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Platform-scoped subscription aggregate (no RLS — Lumie ops can query cross-tenant).
 *
 * <p>State machine (8-flow):
 * <pre>
 *   A: createFree        → ACTIVE (no billing key, no expiresAt)
 *   B: createTrial       → ACTIVE (no billing key, expiresAt = now+30d); expireTrial → EXPIRED
 *   C: subscribe         → ACTIVE (any source state; Trial → Paid)
 *   D: subscribe         → ACTIVE (EXPIRED/CANCELLED → Paid fresh start)
 *   E: changePlan        → ACTIVE (upgrade/same-price; immediate swap; ACTIVE only)
 *   F: scheduleDowngrade → pendingPlan set; applyPendingPlanChange → plan swapped
 *   G: scheduleCancellation → cancelledAt set; finalizeCancellation → CANCELLED
 *   H: undoScheduledCancellation → cancelledAt cleared
 * </pre>
 */
@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_subscriptions_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_subscriptions_tenant_slug", columnList = "tenant_slug"),
        @Index(name = "idx_subscriptions_status", columnList = "status"),
        @Index(name = "idx_subscriptions_billing_key_id", columnList = "billing_key_id"),
        @Index(name = "idx_subscriptions_next_charge_at", columnList = "next_charge_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseEntity {

    private static final int MAX_PAYMENT_FAILURES_BEFORE_PAUSE = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "tenant_slug", nullable = false, length = 30)
    private String tenantSlug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "suspension_reason", length = 50)
    private SuspensionReason suspensionReason;

    @Column(name = "billing_key_id")
    private Long billingKeyId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "next_charge_at")
    private Instant nextChargeAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "billing_cycle_day")
    private Integer billingCycleDay;

    /** Cumulative consecutive payment failures. Reset to 0 on successful charge. */
    @Column(name = "payment_failure_count", nullable = false)
    private int paymentFailureCount;

    /**
     * The plan to switch to at {@link #scheduledChangeAt} (F-flow: deferred downgrade).
     * Null when no scheduled change is pending.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_plan_id")
    private Plan pendingPlan;

    /**
     * Timestamp at which the scheduler applies the pending plan change (F-flow) or
     * finalizes a cancellation (G-flow via {@link #finalizeCancellation}).
     * Null when no scheduled change is pending.
     */
    @Column(name = "scheduled_change_at")
    private Instant scheduledChangeAt;

    @Builder
    private Subscription(Long tenantId, String tenantSlug, Plan plan, Instant startedAt,
                         Instant expiresAt, Integer billingCycleDay) {
        this.tenantId = tenantId;
        this.tenantSlug = tenantSlug;
        this.plan = plan;
        this.status = SubscriptionStatus.ACTIVE;
        this.suspensionReason = SuspensionReason.NONE;
        this.startedAt = startedAt != null ? startedAt : Instant.now();
        this.expiresAt = expiresAt;
        this.paymentFailureCount = 0;
        this.billingCycleDay = billingCycleDay != null ? billingCycleDay
                : this.startedAt.atZone(ZoneOffset.UTC).getDayOfMonth();
    }

    // ─── Factory methods ──────────────────────────────────────────────────────

    /**
     * Create a Free subscription (no billing key, no expiry). Used by TenantCreatedListener
     * until D7 replaces this with {@link #createTrial}.
     */
    public static Subscription createFree(Long tenantId, String tenantSlug, Plan freePlan) {
        return Subscription.builder()
                .tenantId(tenantId)
                .tenantSlug(tenantSlug)
                .plan(freePlan)
                .startedAt(Instant.now())
                .expiresAt(null)
                .build();
    }

    public static Subscription create(Long tenantId, String tenantSlug, Plan plan,
                                       Instant expiresAt) {
        return Subscription.builder()
                .tenantId(tenantId)
                .tenantSlug(tenantSlug)
                .plan(plan)
                .startedAt(Instant.now())
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * Create a Trial subscription on tenant signup. B-flow entry point.
     *
     * <p>Trial = PRO plan (configured at signup) for 30 days, no billing key required.
     * Identifier: ACTIVE + billing_key_id IS NULL (no separate status value).
     * After 30 days the SubscriptionScheduledChangeScheduler calls {@link #expireTrial};
     * users who want to continue subscribe via POST /v1/billing/subscribe (C-flow).
     *
     * @param proPlan the plan the trial grants access to (typically id="PRO")
     */
    public static Subscription createTrial(Long tenantId, String tenantSlug, Plan proPlan) {
        return Subscription.builder()
                .tenantId(tenantId)
                .tenantSlug(tenantSlug)
                .plan(proPlan)
                .startedAt(Instant.now())
                .expiresAt(Instant.now().plus(java.time.Duration.ofDays(30)))
                .build();
        // billingKeyId stays null (trial does not auto-charge)
        // nextChargeAt stays null
        // status = ACTIVE (from builder)
    }

    // ─── Domain methods ───────────────────────────────────────────────────────

    /**
     * Adopt {@code plan} as the paid plan with a registered card. C/D-flow entry point.
     * Used by the /v1/billing/subscribe orchestrator.
     *
     * <p>State changes (regardless of source ACTIVE/PAUSED/PAST_DUE/CANCELLED/EXPIRED):
     * <ul>
     *   <li>status = ACTIVE</li>
     *   <li>plan = plan</li>
     *   <li>billingKeyId = billingKey.getId()</li>
     *   <li>nextChargeAt = nextChargeAt parameter</li>
     *   <li>expiresAt = null (trial expiry cleared; paid is rolling)</li>
     *   <li>cancelledAt = null</li>
     *   <li>pendingPlan / scheduledChangeAt = null (any pending downgrade voided)</li>
     *   <li>suspensionReason = NONE, paymentFailureCount = 0</li>
     * </ul>
     *
     * <p>For CANCELLED / EXPIRED sources (D-flow), startedAt is reset to now (fresh period).
     * For ACTIVE / PAUSED / PAST_DUE sources (C-flow), startedAt is preserved.
     */
    public void subscribe(BillingKey billingKey, Plan plan, Instant nextChargeAt) {
        subscribe(billingKey.getId(), plan, nextChargeAt);
    }

    /**
     * Overload for application-layer callers that hold a billingKeyId (Long) rather than
     * a loaded {@link BillingKey} entity. Same state machine as
     * {@link #subscribe(BillingKey, Plan, Instant)}.
     */
    public void subscribe(Long billingKeyId, Plan plan, Instant nextChargeAt) {
        if (billingKeyId == null) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "subscribe requires a non-null billingKeyId — a paid subscription must "
                            + "have a registered card. Use createTrial(...) for trial provisioning.");
        }
        if (plan == null || nextChargeAt == null) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "subscribe requires non-null plan and nextChargeAt");
        }
        boolean isFreshStart = (this.status == SubscriptionStatus.CANCELLED
                              || this.status == SubscriptionStatus.EXPIRED);
        this.plan = plan;
        this.billingKeyId = billingKeyId;
        this.nextChargeAt = nextChargeAt;
        this.expiresAt = null;
        this.cancelledAt = null;
        this.pendingPlan = null;
        this.scheduledChangeAt = null;
        this.status = SubscriptionStatus.ACTIVE;
        this.suspensionReason = SuspensionReason.NONE;
        this.paymentFailureCount = 0;
        if (isFreshStart) {
            this.startedAt = Instant.now();
        }
    }

    /**
     * Apply {@code newPlan} immediately. E-flow: upgrade or same-price change.
     * Service layer routes upgrade vs downgrade — domain swaps unconditionally.
     *
     * <p>Side-effects:
     * <ul>
     *   <li>plan = newPlan</li>
     *   <li>cancelledAt = null (auto-uncancel — re-subscribing during cancel-grace)</li>
     *   <li>pendingPlan / scheduledChangeAt = null (any pending downgrade voided)</li>
     * </ul>
     *
     * <p>Domain guard: mustBePaid (refuses Trial: billing_key_id IS NULL).
     * ACTIVE only — dunning states (PAUSED/PAST_DUE) use different flows.
     */
    public void changePlan(Plan newPlan) {
        mustBePaid();
        if (this.status != SubscriptionStatus.ACTIVE) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "Can only change plan on ACTIVE subscriptions. Current: " + this.status);
        }
        this.plan = newPlan;
        this.cancelledAt = null;
        this.pendingPlan = null;
        this.scheduledChangeAt = null;
    }

    /**
     * Schedule a downgrade to {@code pendingPlan} at {@code scheduledAt}. F-flow.
     * PRO → BASIC keeps PRO until current cycle end, then BASIC.
     *
     * <p>Service layer computes scheduledAt = current nextChargeAt (when the next charge
     * would have happened at the current plan).
     *
     * <p>Side-effects: pendingPlan, scheduledChangeAt set; cancelledAt cleared
     * (auto-uncancel — re-subscribing during cancel-grace).
     *
     * <p>Domain guard: mustBePaid + ACTIVE only. The pendingPlan must be strictly
     * cheaper than the current plan — caller should route equal/higher to changePlan.
     */
    public void scheduleDowngrade(Plan pendingPlan, Instant scheduledAt) {
        mustBePaid();
        if (this.status != SubscriptionStatus.ACTIVE) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "Can only schedule downgrade on ACTIVE subscriptions. Current: " + this.status);
        }
        if (pendingPlan == null || scheduledAt == null) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "pendingPlan and scheduledAt are required");
        }
        if (!pendingPlan.getMonthlyPrice().isLessThan(this.plan.getMonthlyPrice())) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "scheduleDowngrade requires a lower-priced plan");
        }
        this.pendingPlan = pendingPlan;
        this.scheduledChangeAt = scheduledAt;
        this.cancelledAt = null;
    }

    /**
     * Mark the subscription for cancellation at the end of the current paid period. G-flow.
     * cancelledAt = now(); status stays ACTIVE; service remains usable until nextChargeAt.
     * The daily SubscriptionScheduledChangeScheduler calls {@link #finalizeCancellation}
     * when nextChargeAt passes.
     *
     * <p>Domain guard: mustBePaid (Trial cannot be cancelled — no cancel button for trial).
     */
    public void scheduleCancellation() {
        mustBePaid();
        if (this.status != SubscriptionStatus.ACTIVE) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "Can only schedule cancellation on ACTIVE subscriptions. Current: " + this.status);
        }
        if (this.cancelledAt != null) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "Subscription is already scheduled for cancellation");
        }
        this.cancelledAt = Instant.now();
    }

    /**
     * Undo a scheduled cancellation. H-flow: cancelledAt = null; nothing else changes.
     */
    public void undoScheduledCancellation() {
        if (this.cancelledAt == null) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "No scheduled cancellation to undo");
        }
        this.cancelledAt = null;
    }

    /**
     * Trial expiry. B-flow continuation. Called by SubscriptionScheduledChangeScheduler when
     * billing_key_id IS NULL AND expires_at &lt;= now AND status = ACTIVE.
     *
     * <p>Defensive guards inside the method (idempotent re-run safe).
     */
    public void expireTrial() {
        if (this.billingKeyId != null) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "expireTrial only applies to trial subscriptions (no billing key)");
        }
        if (this.status != SubscriptionStatus.ACTIVE) {
            return;  // already transitioned, idempotent
        }
        this.status = SubscriptionStatus.EXPIRED;
    }

    /**
     * Apply pending downgrade. F-flow continuation. Called when scheduledChangeAt &lt;= now.
     * Swaps to pendingPlan and clears pending state. Idempotent.
     */
    public void applyPendingPlanChange() {
        if (this.pendingPlan == null) {
            return;  // nothing to apply, idempotent
        }
        this.plan = this.pendingPlan;
        this.pendingPlan = null;
        this.scheduledChangeAt = null;
    }

    /**
     * Finalize cancellation. G-flow continuation. Called when cancel-grace period ends
     * (nextChargeAt &lt;= now AND cancelledAt IS NOT NULL).
     * Transitions status to CANCELLED. Idempotent.
     */
    public void finalizeCancellation() {
        if (this.cancelledAt == null) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "Cannot finalize cancellation: cancelled_at is null");
        }
        if (this.status == SubscriptionStatus.CANCELLED) {
            return;  // already finalized, idempotent
        }
        this.status = SubscriptionStatus.CANCELLED;
    }

    public void markPastDue() {
        validateActive();
        this.status = SubscriptionStatus.PAST_DUE;
    }

    public void pause(SuspensionReason reason) {
        if (this.status != SubscriptionStatus.ACTIVE && this.status != SubscriptionStatus.PAST_DUE) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "Can only pause ACTIVE or PAST_DUE subscriptions. Current: " + this.status);
        }
        this.status = SubscriptionStatus.PAUSED;
        this.suspensionReason = reason != null ? reason : SuspensionReason.MANUAL_PAUSE;
    }

    public void resume() {
        if (this.status != SubscriptionStatus.PAUSED) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "Can only resume a PAUSED subscription. Current: " + this.status);
        }
        this.status = SubscriptionStatus.ACTIVE;
        this.suspensionReason = SuspensionReason.NONE;
        this.paymentFailureCount = 0;
    }

    public void attachBillingKey(Long billingKeyId) {
        this.billingKeyId = billingKeyId;
    }

    /**
     * Records a failed charge attempt.
     * After {@value #MAX_PAYMENT_FAILURES_BEFORE_PAUSE} consecutive failures,
     * the subscription is automatically PAUSED with reason PAYMENT_FAILED_3X.
     */
    public void chargeAttemptFailed() {
        this.paymentFailureCount++;
        if (this.paymentFailureCount >= MAX_PAYMENT_FAILURES_BEFORE_PAUSE) {
            pause(SuspensionReason.PAYMENT_FAILED_3X);
        } else {
            if (this.status == SubscriptionStatus.ACTIVE) {
                this.status = SubscriptionStatus.PAST_DUE;
            }
        }
    }

    /** Called after a successful charge to reset the failure counter and mark ACTIVE. */
    public void chargeSucceeded(Instant nextChargeAt) {
        this.paymentFailureCount = 0;
        this.status = SubscriptionStatus.ACTIVE;
        this.suspensionReason = SuspensionReason.NONE;
        this.nextChargeAt = nextChargeAt;
    }

    public void expire() {
        if (this.status == SubscriptionStatus.CANCELLED) {
            return;
        }
        this.status = SubscriptionStatus.EXPIRED;
    }

    public void updateNextChargeAt(Instant nextChargeAt) {
        this.nextChargeAt = nextChargeAt;
    }

    // ─── Query methods ────────────────────────────────────────────────────────

    public boolean isActive() {
        return this.status == SubscriptionStatus.ACTIVE;
    }

    public boolean isPaused() {
        return this.status == SubscriptionStatus.PAUSED;
    }

    public boolean isExpired() {
        if (this.status == SubscriptionStatus.EXPIRED) {
            return true;
        }
        return this.expiresAt != null && Instant.now().isAfter(this.expiresAt);
    }

    public boolean hasBillingKey() {
        return this.billingKeyId != null;
    }

    /** True when this is a trial subscription (ACTIVE, no billing key, has expiresAt). */
    public boolean isTrial() {
        return this.billingKeyId == null && this.status == SubscriptionStatus.ACTIVE
                && this.expiresAt != null;
    }

    /** True when a cancellation has been scheduled (cancel-grace period active). */
    public boolean isCancellationScheduled() {
        return this.cancelledAt != null && this.status == SubscriptionStatus.ACTIVE;
    }

    /** True when a plan downgrade is scheduled for a future date. */
    public boolean isDowngradeScheduled() {
        return this.pendingPlan != null;
    }

    public String getPlanId() {
        return plan.getId();
    }

    public String getPlanName() {
        return plan.getName();
    }

    public com.lumie.billing.domain.vo.PlanLimits getLimits() {
        return plan.toLimits();
    }

    public boolean checkQuota(com.lumie.common.quota.MetricType metricType, long currentUsage) {
        return getLimits().isWithinLimit(metricType, currentUsage);
    }

    // ─── equals / hashCode (ID-based) ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subscription other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void validateActive() {
        if (this.status != SubscriptionStatus.ACTIVE) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "Subscription must be ACTIVE for this operation. Current: " + this.status);
        }
    }

    /**
     * Guard: this operation requires a paid subscription (billingKeyId != null).
     * Trials and Free subscriptions are rejected.
     */
    private void mustBePaid() {
        if (this.billingKeyId == null) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "This operation requires a paid subscription. Trial subscriptions cannot be modified this way.");
        }
    }
}
