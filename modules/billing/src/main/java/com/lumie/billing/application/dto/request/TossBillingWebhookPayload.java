package com.lumie.billing.application.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Toss billing/subscription webhook payload.
 * Uses @JsonIgnoreProperties to tolerate schema evolution from Toss.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossBillingWebhookPayload(
        String eventType,
        String createdAt,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            String orderId,
            String paymentKey,
            String billingKey,
            String customerKey,
            String status,
            Long totalAmount
    ) {}

    public boolean isBillingKeyIssued() {
        return "BILLING_KEY.ISSUED".equals(eventType);
    }

    public boolean isPaymentDone() {
        return "PAYMENT.STATUS_CHANGED".equals(eventType)
                && data != null && "DONE".equals(data.status());
    }

    public boolean isPaymentFailed() {
        return "PAYMENT.STATUS_CHANGED".equals(eventType)
                && data != null && "ABORTED".equals(data.status());
    }
}
