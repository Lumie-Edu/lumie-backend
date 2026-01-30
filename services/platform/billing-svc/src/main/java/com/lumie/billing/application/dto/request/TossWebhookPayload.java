package com.lumie.billing.application.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossWebhookPayload(
        String eventType,
        String createdAt,
        TossPaymentData data
) {
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TossPaymentData(
            String paymentKey,
            String orderId,
            String orderName,
            String status,
            long totalAmount,
            String method,
            String requestedAt,
            String approvedAt,
            TossCardInfo card,
            TossCancellation[] cancels
    ) {}

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TossCardInfo(
            String company,
            String number,
            String cardType,
            String ownerType
    ) {}

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TossCancellation(
            String cancelReason,
            long cancelAmount,
            String canceledAt
    ) {}

    public boolean isPaymentDone() {
        return "DONE".equals(data.status());
    }

    public boolean isPaymentCanceled() {
        return "CANCELED".equals(data.status());
    }
}
