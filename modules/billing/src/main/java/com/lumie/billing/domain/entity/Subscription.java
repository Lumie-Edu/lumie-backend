package com.lumie.billing.domain.entity;

import com.lumie.billing.domain.exception.InvalidSubscriptionStateException;
import com.lumie.billing.domain.vo.MetricType;
import com.lumie.billing.domain.vo.PlanLimits;
import com.lumie.billing.domain.vo.SubscriptionStatus;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZoneOffset;

@Entity
@Table(name = "subscriptions", indexes = {
        @Index(name = "idx_subscriptions_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_subscriptions_tenant_slug", columnList = "tenant_slug")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "billing_cycle_day")
    private Integer billingCycleDay;

    @Column(name = "paddle_subscription_id", length = 50)
    private String paddleSubscriptionId;

    @Column(name = "paddle_customer_id", length = 50)
    private String paddleCustomerId;

    @Column(name = "billing_cycle", length = 10)
    private String billingCycle;

    @Builder
    private Subscription(Long tenantId, String tenantSlug, Plan plan, Instant startedAt,
                         Instant expiresAt, Integer billingCycleDay) {
        this.tenantId = tenantId;
        this.tenantSlug = tenantSlug;
        this.plan = plan;
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = startedAt != null ? startedAt : Instant.now();
        this.expiresAt = expiresAt;
        this.billingCycleDay = billingCycleDay != null ? billingCycleDay
                : this.startedAt.atZone(ZoneOffset.UTC).getDayOfMonth();
    }

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

    public void changePlan(Plan newPlan) {
        validateActive();
        this.plan = newPlan;
    }

    public void cancel() {
        if (this.status != SubscriptionStatus.ACTIVE && this.status != SubscriptionStatus.PAST_DUE) {
            throw new InvalidSubscriptionStateException(
                    "Can only cancel active or past_due subscriptions. Current status: " + this.status);
        }
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    public void markPastDue() {
        validateActive();
        this.status = SubscriptionStatus.PAST_DUE;
    }

    public void resumeFromPastDue() {
        if (this.status != SubscriptionStatus.PAST_DUE) {
            throw new InvalidSubscriptionStateException(
                    "Can only resume from past_due state. Current status: " + this.status);
        }
        this.status = SubscriptionStatus.ACTIVE;
    }

    public void linkPaddle(String paddleSubscriptionId, String paddleCustomerId) {
        this.paddleSubscriptionId = paddleSubscriptionId;
        this.paddleCustomerId = paddleCustomerId;
    }

    public void expire() {
        if (this.status == SubscriptionStatus.CANCELLED) {
            return;
        }
        this.status = SubscriptionStatus.EXPIRED;
    }

    public void reactivate(Plan plan, Instant newExpiresAt) {
        if (this.status != SubscriptionStatus.CANCELLED && this.status != SubscriptionStatus.EXPIRED) {
            throw new InvalidSubscriptionStateException(
                    "Can only reactivate cancelled or expired subscriptions");
        }
        this.plan = plan;
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = Instant.now();
        this.expiresAt = newExpiresAt;
        this.cancelledAt = null;
    }

    public void extendExpiration(Instant newExpiresAt) {
        validateActive();
        if (newExpiresAt != null && (this.expiresAt == null || newExpiresAt.isAfter(this.expiresAt))) {
            this.expiresAt = newExpiresAt;
        }
    }

    private void validateActive() {
        if (this.status != SubscriptionStatus.ACTIVE) {
            throw new InvalidSubscriptionStateException(
                    "Subscription is not active. Current status: " + this.status);
        }
    }

    public boolean isActive() {
        return this.status == SubscriptionStatus.ACTIVE;
    }

    public boolean isExpired() {
        if (this.status == SubscriptionStatus.EXPIRED) {
            return true;
        }
        return this.expiresAt != null && Instant.now().isAfter(this.expiresAt);
    }

    public PlanLimits getLimits() {
        return plan.toLimits();
    }

    public boolean checkQuota(MetricType metricType, long currentUsage) {
        return getLimits().isWithinLimit(metricType, currentUsage);
    }

    public String getPlanId() {
        return plan.getId();
    }

    public String getPlanName() {
        return plan.getName();
    }
}
