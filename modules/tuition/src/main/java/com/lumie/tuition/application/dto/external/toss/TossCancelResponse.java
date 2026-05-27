package com.lumie.tuition.application.dto.external.toss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Toss Payments /v1/payments/{paymentKey}/cancel success response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossCancelResponse(
        String paymentKey,
        String orderId,
        String status,
        List<CancelDetail> cancels
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CancelDetail(
            BigDecimal cancelAmount,
            String cancelReason,
            String canceledAt
    ) {}
}
