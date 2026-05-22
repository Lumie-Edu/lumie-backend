package com.lumie.billing.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
public record PaymentResponse(
        @Schema(nullable = true) String paymentKey,
        @Schema(nullable = true) String orderId,
        @Schema(nullable = true) String orderName,
        String status,
        long totalAmount,
        @Schema(nullable = true) String method,
        @Schema(nullable = true) Instant requestedAt,
        @Schema(nullable = true) Instant approvedAt,
        @Schema(nullable = true) CardInfo card,
        @Schema(nullable = true) String message
) {
    @Builder
    public record CardInfo(
            @Schema(nullable = true) String company,
            @Schema(nullable = true) String number,
            @Schema(nullable = true) String cardType,
            @Schema(nullable = true) String ownerType
    ) {}

    public boolean isSuccess() {
        return "DONE".equals(status);
    }
}
