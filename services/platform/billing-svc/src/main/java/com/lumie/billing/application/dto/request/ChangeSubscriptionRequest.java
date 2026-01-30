package com.lumie.billing.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ChangeSubscriptionRequest(
        @NotBlank(message = "New plan ID is required")
        String newPlanId
) {
}
