package com.lumie.tuition.application.dto.external.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Toss Payments /v1/payments/* success response.
 * Only fields required by the tuition domain are mapped.
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
