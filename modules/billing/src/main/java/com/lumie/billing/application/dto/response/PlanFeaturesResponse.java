package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.vo.PlanFeatures;

import java.util.List;

public record PlanFeaturesResponse(
        boolean advancedAnalytics,
        boolean prioritySupport,
        boolean apiAccess,
        boolean whiteLabeling,
        List<String> additionalFeatures
) {
    public static PlanFeaturesResponse from(PlanFeatures vo) {
        if (vo == null) {
            return null;
        }
        return new PlanFeaturesResponse(
                vo.advancedAnalytics(),
                vo.prioritySupport(),
                vo.apiAccess(),
                vo.whiteLabeling(),
                vo.additionalFeatures() == null ? List.of() : List.copyOf(vo.additionalFeatures())
        );
    }
}
