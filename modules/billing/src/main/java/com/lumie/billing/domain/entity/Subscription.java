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
 * <p>State machine:
 * <pre>
 *   PENDING → ACTIVE → PAUSED ⇄ ACTIVE
 *   ACTIVE  → PAST_DUE → ACTIVE (payment retry)
 *   ACTIVE | PAST_DUE | PAUSED → CANCELLED
 *   ACTIVE | PAUSED → EXPIRED
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

    // ─── Domain methods ───────────────────────────────────────────────────────

    public void changePlan(Plan newPlan) {
        validateActive();
        this.plan = newPlan;
    }

    public void cancel() {
        if (this.status == SubscriptionStatus.CANCELLED) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "Subscription is already cancelled");
        }
        if (this.status == SubscriptionStatus.EXPIRED) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "Cannot cancel an expired subscription");
        }
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.suspensionReason = SuspensionReason.NONE;
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

    public void reactivate(Plan plan, Instant newExpiresAt) {
        if (this.status != SubscriptionStatus.CANCELLED && this.status != SubscriptionStatus.EXPIRED) {
            throw new BillingException(BillingErrorCode.INVALID_SUBSCRIPTION_STATE,
                    "Can only reactivate CANCELLED or EXPIRED subscriptions. Current: " + this.status);
        }
        this.plan = plan;
        this.status = SubscriptionStatus.ACTIVE;
        this.suspensionReason = SuspensionReason.NONE;
        this.startedAt = Instant.now();
        this.expiresAt = newExpiresAt;
        this.cancelledAt = null;
        this.paymentFailureCount = 0;
    }

    public void extendExpiration(Instant newExpiresAt) {
        validateActive();
        if (newExpiresAt != null && (this.expiresAt == null || newExpiresAt.isAfter(this.expiresAt))) {
            this.expiresAt = newExpiresAt;
        }
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
}
