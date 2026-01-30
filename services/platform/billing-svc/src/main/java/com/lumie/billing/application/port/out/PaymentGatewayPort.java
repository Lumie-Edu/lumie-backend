package com.lumie.billing.application.port.out;

import com.lumie.billing.application.dto.response.PaymentResponse;

public interface PaymentGatewayPort {
    PaymentResponse confirmPayment(String paymentKey, String orderId, long amount);
    PaymentResponse cancelPayment(String paymentKey, String cancelReason);
    PaymentResponse getPayment(String paymentKey);
}
