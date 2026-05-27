package com.lumie.tuition.application.dto.response;

/**
 * Returned by requestPayment — the FE redirects the user to checkoutUrl
 * to complete the Toss Payments widget flow.
 */
public record PaymentCheckoutResponse(
        Long paymentId,
        String orderId,
        String checkoutUrl
) {}
