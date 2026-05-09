package com.lumie.billing.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record ConfirmPaymentRequest(
        @NotBlank(message = "Payment key is required")
        String paymentKey,

        @NotBlank(message = "Order ID is required")
        String orderId,

        @Positive(message = "Amount must be positive")
        long amount
) {
}
