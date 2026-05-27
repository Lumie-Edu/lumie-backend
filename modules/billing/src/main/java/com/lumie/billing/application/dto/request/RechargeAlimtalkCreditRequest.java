package com.lumie.billing.application.dto.request;

import jakarta.validation.constraints.Min;

public record RechargeAlimtalkCreditRequest(
        @Min(1) int packageSize
) {}
