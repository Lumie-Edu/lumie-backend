package com.lumie.billing.application.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record PaymentResponse(
        String paymentKey,
        String orderId,
        String orderName,
        String status,
        long totalAmount,
        String method,
        Instant requestedAt,
        Instant approvedAt,
        CardInfo card,
        String message
) {
    @Builder
    public record CardInfo(
            String company,
            String number,
            String cardType,
            String ownerType
    ) {}

    public boolean isSuccess() {
        return "DONE".equals(status);
    }
}
