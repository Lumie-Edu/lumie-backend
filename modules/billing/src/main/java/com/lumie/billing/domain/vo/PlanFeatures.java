package com.lumie.billing.domain.vo;

import lombok.Builder;

import java.util.List;

@Builder
public record PlanFeatures(
        boolean customDomains,
        boolean advancedAnalytics,
        boolean prioritySupport,
        boolean apiAccess,
        boolean whiteLabeling,
        List<String> additionalFeatures
) {
    public static PlanFeatures free() {
        return PlanFeatures.builder()
                .customDomains(false)
                .advancedAnalytics(false)
                .prioritySupport(false)
                .apiAccess(false)
                .whiteLabeling(false)
                .additionalFeatures(List.of())
                .build();
    }

    public static PlanFeatures basic() {
        return PlanFeatures.builder()
                .customDomains(false)
                .advancedAnalytics(false)
                .prioritySupport(false)
                .apiAccess(true)
                .whiteLabeling(false)
                .additionalFeatures(List.of("email_support"))
                .build();
    }

    public static PlanFeatures pro() {
        return PlanFeatures.builder()
                .customDomains(true)
                .advancedAnalytics(true)
                .prioritySupport(true)
                .apiAccess(true)
                .whiteLabeling(false)
                .additionalFeatures(List.of("email_support", "phone_support", "custom_reports"))
                .build();
    }

    public static PlanFeatures enterprise() {
        return PlanFeatures.builder()
                .customDomains(true)
                .advancedAnalytics(true)
                .prioritySupport(true)
                .apiAccess(true)
                .whiteLabeling(true)
                .additionalFeatures(List.of("email_support", "phone_support", "custom_reports",
                        "dedicated_account_manager", "sla_guarantee", "custom_integrations"))
                .build();
    }
}
