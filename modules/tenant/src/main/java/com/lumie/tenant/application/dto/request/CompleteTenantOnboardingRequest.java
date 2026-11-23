package com.lumie.tenant.application.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CompleteTenantOnboardingRequest(
        @Size(max = 30, message = "Phone must be at most 30 characters")
        String phone
) {
}
