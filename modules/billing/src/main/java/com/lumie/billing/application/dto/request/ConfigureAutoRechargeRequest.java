package com.lumie.billing.application.dto.request;

import jakarta.validation.constraints.Min;

public record ConfigureAutoRechargeRequest(
        @Min(1) int threshold,
        @Min(1) int packageSize
) {}
