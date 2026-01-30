package com.lumie.billing.adapter.out.external;

import com.lumie.billing.application.dto.response.PaymentResponse;
import com.lumie.billing.application.port.out.PaymentGatewayPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Stub implementation of PaymentGatewayPort.
 * TODO: Implement actual Toss Payments integration later.
 *
 * When implementing:
 * 1. Add toss.payments.secret-key and toss.payments.client-key to application.yaml
 * 2. Register secrets in Vault: secret/applications/toss
 * 3. Update common-values.yaml with VaultStaticSecret for toss credentials
 */
@Slf4j
@Component
public class TossPaymentClient implements PaymentGatewayPort {

    @Override
    public PaymentResponse confirmPayment(String paymentKey, String orderId, long amount) {
        log.warn("STUB: Toss payment integration not implemented. " +
                "Returning mock success for paymentKey={}, orderId={}, amount={}",
                paymentKey, orderId, amount);

        return PaymentResponse.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .orderName("Mock Order")
                .status("DONE")
                .totalAmount(amount)
                .method("CARD")
                .requestedAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .card(null)
                .message("STUB: Payment integration pending")
                .build();
    }

    @Override
    public PaymentResponse cancelPayment(String paymentKey, String cancelReason) {
        log.warn("STUB: Toss payment integration not implemented. " +
                "Returning mock cancellation for paymentKey={}, reason={}",
                paymentKey, cancelReason);

        return PaymentResponse.builder()
                .paymentKey(paymentKey)
                .orderId("mock-order-id")
                .orderName("Mock Order")
                .status("CANCELED")
                .totalAmount(0)
                .method("CARD")
                .requestedAt(LocalDateTime.now())
                .approvedAt(null)
                .card(null)
                .message("STUB: Payment integration pending")
                .build();
    }

    @Override
    public PaymentResponse getPayment(String paymentKey) {
        log.warn("STUB: Toss payment integration not implemented. " +
                "Returning mock payment for paymentKey={}", paymentKey);

        return PaymentResponse.builder()
                .paymentKey(paymentKey)
                .orderId("mock-order-id")
                .orderName("Mock Order")
                .status("DONE")
                .totalAmount(0)
                .method("CARD")
                .requestedAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .card(null)
                .message("STUB: Payment integration pending")
                .build();
    }
}
