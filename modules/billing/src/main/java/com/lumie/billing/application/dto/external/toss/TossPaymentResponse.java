package com.lumie.billing.application.dto.external.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Toss Payments /v1/payments/* success response.
 * Only fields needed by billing are mapped; unknown fields are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossPaymentResponse(
        String paymentKey,
        String orderId,
        String orderName,
        String status,
        String method,
        BigDecimal totalAmount,
        String approvedAt
) {}
