package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.entity.Plan;
import com.lumie.billing.domain.vo.PlanFeatures;
import com.lumie.billing.domain.vo.PlanLimits;
import lombok.Builder;

@Builder
public record PlanResponse(
        String id,
        String name,
        String description,
        long monthlyPrice,
        long yearlyPrice,
        PlanLimits limits,
        PlanFeatures features,
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
                .limits(plan.toLimits())
                .features(plan.toFeatures())
                .displayOrder(plan.getDisplayOrder())
                .active(plan.isActive())
                .build();
    }
}
