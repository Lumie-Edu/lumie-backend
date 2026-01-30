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

import java.time.LocalDateTime;

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
    private LocalDateTime startedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "billing_cycle_day")
    private Integer billingCycleDay;

    @Builder
    private Subscription(Long tenantId, String tenantSlug, Plan plan, LocalDateTime startedAt,
                         LocalDateTime expiresAt, Integer billingCycleDay) {
        this.tenantId = tenantId;
        this.tenantSlug = tenantSlug;
        this.plan = plan;
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = startedAt != null ? startedAt : LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.billingCycleDay = billingCycleDay != null ? billingCycleDay : this.startedAt.getDayOfMonth();
    }

    public static Subscription createFree(Long tenantId, String tenantSlug, Plan freePlan) {
        return Subscription.builder()
                .tenantId(tenantId)
                .tenantSlug(tenantSlug)
                .plan(freePlan)
                .startedAt(LocalDateTime.now())
                .expiresAt(null)
                .build();
    }

    public static Subscription create(Long tenantId, String tenantSlug, Plan plan,
                                       LocalDateTime expiresAt) {
        return Subscription.builder()
                .tenantId(tenantId)
                .tenantSlug(tenantSlug)
                .plan(plan)
                .startedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .build();
    }

    public void changePlan(Plan newPlan) {
        validateActive();
        this.plan = newPlan;
    }

    public void cancel() {
        validateActive();
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public void expire() {
        if (this.status == SubscriptionStatus.CANCELLED) {
            return;
        }
        this.status = SubscriptionStatus.EXPIRED;
    }

    public void reactivate(Plan plan, LocalDateTime newExpiresAt) {
        if (this.status != SubscriptionStatus.CANCELLED && this.status != SubscriptionStatus.EXPIRED) {
            throw new InvalidSubscriptionStateException(
                    "Can only reactivate cancelled or expired subscriptions");
        }
        this.plan = plan;
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = LocalDateTime.now();
        this.expiresAt = newExpiresAt;
        this.cancelledAt = null;
    }

    public void extendExpiration(LocalDateTime newExpiresAt) {
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
        return this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt);
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
