package com.lumie.billing.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateSubscriptionRequest(
        @NotNull(message = "Tenant ID is required")
        Long tenantId,

        @NotBlank(message = "Tenant slug is required")
        String tenantSlug,

        @NotBlank(message = "Plan ID is required")
        String planId,

        Integer billingCycleMonths
) {
    public CreateSubscriptionRequest {
        if (billingCycleMonths == null) {
            billingCycleMonths = 1;
        }
    }
}
