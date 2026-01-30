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

@Entity
@Table(name = "plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan extends BaseEntity {

    @Id
    @Column(name = "id", length = 20)
    private String id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "monthly_price"))
    private Money monthlyPrice;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "yearly_price"))
    private Money yearlyPrice;

    @Column(name = "max_students", nullable = false)
    private int maxStudents;

    @Column(name = "max_academies", nullable = false)
    private int maxAcademies;

    @Column(name = "max_admins", nullable = false)
    private int maxAdmins;

    @Column(name = "omr_monthly_quota", nullable = false)
    private int omrMonthlyQuota;

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
                 int maxStudents, int maxAcademies, int maxAdmins, int omrMonthlyQuota,
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

    public PlanLimits toLimits() {
        return PlanLimits.builder()
                .maxStudents(maxStudents)
                .maxAcademies(maxAcademies)
                .maxAdmins(maxAdmins)
                .omrMonthlyQuota(omrMonthlyQuota)
                .build();
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
        return monthlyPrice.isZero();
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
