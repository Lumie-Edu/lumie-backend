package com.lumie.billing.domain.entity;

import com.lumie.billing.domain.vo.Money;
import com.lumie.billing.domain.vo.PlanFeatures;
import com.lumie.billing.domain.vo.PlanLimits;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * Platform-scoped plan configuration (no RLS — Lumie ops managed).
 * deactivate()/activate() domain methods require optimistic locking (@Version).
 */
@Entity
@Table(name = "plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan extends BaseEntity {

    @Id
    @Column(name = "id", length = 20)
    private String id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "monthly_price"))
    private Money monthlyPrice;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "yearly_price"))
    private Money yearlyPrice;

    /** NULL means unlimited (no cap enforced). */
    @Column(name = "max_students")
    private Integer maxStudents;

    /** NULL means unlimited. */
    @Column(name = "max_academies")
    private Integer maxAcademies;

    /** NULL means unlimited. */
    @Column(name = "max_admins")
    private Integer maxAdmins;

    /** NULL means unlimited. */
    @Column(name = "omr_monthly_quota")
    private Integer omrMonthlyQuota;

    @Column(name = "custom_domains", nullable = false)
    private boolean customDomains;

    @Column(name = "advanced_analytics", nullable = false)
    private boolean advancedAnalytics;

    @Column(name = "priority_support", nullable = false)
    private boolean prioritySupport;

    @Column(name = "api_access", nullable = false)
    private boolean apiAccess;

    @Column(name = "white_labeling", nullable = false)
    private boolean whiteLabeling;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Builder
    private Plan(String id, String name, String description, Money monthlyPrice, Money yearlyPrice,
                 Integer maxStudents, Integer maxAcademies, Integer maxAdmins, Integer omrMonthlyQuota,
                 boolean customDomains, boolean advancedAnalytics, boolean prioritySupport,
                 boolean apiAccess, boolean whiteLabeling, int displayOrder) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.monthlyPrice = monthlyPrice;
        this.yearlyPrice = yearlyPrice;
        this.maxStudents = maxStudents;
        this.maxAcademies = maxAcademies;
        this.maxAdmins = maxAdmins;
        this.omrMonthlyQuota = omrMonthlyQuota;
        this.customDomains = customDomains;
        this.advancedAnalytics = advancedAnalytics;
        this.prioritySupport = prioritySupport;
        this.apiAccess = apiAccess;
        this.whiteLabeling = whiteLabeling;
        this.displayOrder = displayOrder;
        this.active = true;
    }

    // ─── Domain methods ───────────────────────────────────────────────────────

    public PlanLimits toLimits() {
        return PlanLimits.builder()
                .maxStudents(maxStudents)
                .maxAcademies(maxAcademies)
                .maxAdmins(maxAdmins)
                .omrMonthlyQuota(omrMonthlyQuota)
                .build();
    }

    /** True when this plan has no cap on the given dimension (NULL = unlimited). */
    public boolean isUnlimited() {
        return maxStudents == null && maxAcademies == null && maxAdmins == null && omrMonthlyQuota == null;
    }

    public PlanFeatures toFeatures() {
        return PlanFeatures.builder()
                .customDomains(customDomains)
                .advancedAnalytics(advancedAnalytics)
                .prioritySupport(prioritySupport)
                .apiAccess(apiAccess)
                .whiteLabeling(whiteLabeling)
                .additionalFeatures(java.util.List.of())
                .build();
    }

    public boolean isFree() {
        return monthlyPrice == null || monthlyPrice.isZero();
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    // ─── equals / hashCode (ID-based) ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Plan other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
