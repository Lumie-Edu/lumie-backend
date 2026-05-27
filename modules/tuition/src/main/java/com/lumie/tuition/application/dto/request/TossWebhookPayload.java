package com.lumie.tuition.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Inbound payload from Toss Payments webhook (tuition domain).
 * Full event schema: https://docs.tosspayments.com/guides/webhook
 */
public record TossWebhookPayload(
        @JsonProperty("eventType") String eventType,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("data") PaymentData data
) {
    public boolean isPaymentDone() {
        return "PAYMENT_STATUS_CHANGED".equals(eventType)
                && data != null && "DONE".equals(data.status());
    }

    public boolean isPaymentCanceled() {
        return "PAYMENT_STATUS_CHANGED".equals(eventType)
                && data != null && "CANCELED".equals(data.status());
    }

    public record PaymentData(
            @JsonProperty("paymentKey") String paymentKey,
            @JsonProperty("orderId") String orderId,
            @JsonProperty("status") String status,
            @JsonProperty("totalAmount") BigDecimal totalAmount,
            @JsonProperty("method") String method,
            @JsonProperty("approvedAt") Instant approvedAt
    ) {}
}
