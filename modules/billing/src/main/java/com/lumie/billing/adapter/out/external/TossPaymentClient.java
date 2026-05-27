package com.lumie.billing.adapter.out.external;

import com.lumie.billing.application.port.out.PaymentGatewayPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stub implementation of PaymentGatewayPort (Toss Payments subscription billing).
 *
 * TODO (Task 5 PoC): implement actual Toss Payments integration.
 * When implementing:
 * 1. Add to Vault: secret/applications/toss-billing (secretKey, clientKey, webhookSecret)
 * 2. Update common-values.yaml with VaultStaticSecret for toss-billing credentials
 * 3. Confirm endpoint: POST https://api.tosspayments.com/v1/payments/confirm
 * 4. Cancel endpoint: POST https://api.tosspayments.com/v1/payments/{paymentKey}/cancel
 * 5. Billing key issue: POST https://api.tosspayments.com/v1/billing/authorizations/issue
 * 6. Billing key charge: POST https://api.tosspayments.com/v1/billing/{billingKey}
 * 7. Webhook verify: HMAC-SHA256(key=webhookSecret, message=rawBody)
 *
 * NOTE: All calls must be made OUTSIDE @Transactional boundaries (HARD_RULES).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentClient implements PaymentGatewayPort {

    @Override
    public PaymentConfirmResult confirmPayment(String paymentKey, String orderId, BigDecimal amount) {
        log.warn("STUB: TossPaymentClient.confirmPayment not implemented. orderId={}", orderId);
        // TODO: POST /v1/payments/confirm with Basic Auth (Base64(secretKey:))
        return new PaymentConfirmResult(true, paymentKey, orderId, amount, "CARD",
                "STUB: Toss billing integration pending");
    }

    @Override
    public PaymentCancelResult cancelPayment(String paymentKey, String reason) {
        log.warn("STUB: TossPaymentClient.cancelPayment not implemented. paymentKey={}", paymentKey);
        // TODO: POST /v1/payments/{paymentKey}/cancel
        return new PaymentCancelResult(true, BigDecimal.ZERO,
                "STUB: Toss billing integration pending");
    }

    @Override
    public BillingKeyIssueResult issueBillingKey(String authKey, String customerKey) {
        log.warn("STUB: TossPaymentClient.issueBillingKey not implemented. customerKey={}", customerKey);
        // TODO: POST /v1/billing/authorizations/issue
        return new BillingKeyIssueResult(true,
                "STUB_BILLING_KEY_" + customerKey,
                "4242-****-****-4242",
                "STUB_CARD",
                customerKey,
                "STUB: Toss billing key integration pending");
    }

    @Override
    public BillingChargeResult chargeWithBillingKey(String billingKey, BigDecimal amount,
                                                     String orderId, String orderName) {
        log.warn("STUB: TossPaymentClient.chargeWithBillingKey not implemented. orderId={}", orderId);
        // TODO: POST /v1/billing/{billingKey}
        return new BillingChargeResult(true,
                "STUB_PAYMENT_KEY_" + orderId,
                orderId,
                amount,
                "STUB: Toss billing charge integration pending");
    }

    @Override
    public WebhookVerificationResult verifyWebhook(String signature, String rawBody) {
        log.warn("STUB: TossPaymentClient.verifyWebhook — accepting all signatures in stub mode");
        // TODO: implement HMAC-SHA256 verification with billing.toss.webhookSecret
        return new WebhookVerificationResult(true, "PAYMENT_STATUS_CHANGED", "stub-order-id");
    }
}
