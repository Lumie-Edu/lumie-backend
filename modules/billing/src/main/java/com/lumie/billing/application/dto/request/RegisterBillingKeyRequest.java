package com.lumie.billing.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterBillingKeyRequest(
        @NotBlank String authKey,
        @NotBlank String customerKey
) {}
