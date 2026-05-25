package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.entity.Plan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PlanResponse(
        String id,
        String name,
        @Schema(nullable = true) String description,
        long monthlyPrice,
        long yearlyPrice,
        PlanLimitsResponse limits,
        PlanFeaturesResponse features,
        int displayOrder,
        boolean active
) {
    public static PlanResponse from(Plan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .monthlyPrice(plan.getMonthlyPrice().toLong())
                .yearlyPrice(plan.getYearlyPrice().toLong())
                .limits(PlanLimitsResponse.from(plan.toLimits()))
                .features(PlanFeaturesResponse.from(plan.toFeatures()))
                .displayOrder(plan.getDisplayOrder())
                .active(plan.isActive())
                .build();
    }
}
