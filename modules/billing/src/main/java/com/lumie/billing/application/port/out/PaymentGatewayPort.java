package com.lumie.billing.application.port.out;

import java.math.BigDecimal;

/**
 * Toss Payments outbound port for subscription billing operations.
 * All implementations must be called OUTSIDE @Transactional boundaries (HARD_RULES).
 */
public interface PaymentGatewayPort {

    /** Confirms a one-time payment (e.g. manual upgrade, alimtalk recharge). */
    PaymentConfirmResult confirmPayment(String paymentKey, String orderId, BigDecimal amount);

    /** Cancels/refunds a payment fully or partially. */
    PaymentCancelResult cancelPayment(String paymentKey, String reason);

    /** Issues a Toss billing key from the one-time auth code returned after card registration. */
    BillingKeyIssueResult issueBillingKey(String authKey, String customerKey);

    /**
     * Charges a registered billing key — used for automatic monthly subscription billing.
     *
     * @param billingKey  the pg_billing_key stored in BillingKey entity
     * @param amount      amount in KRW
     * @param orderId     unique per-charge order ID
     * @param orderName   human-readable description (shows on card statement)
     */
    BillingChargeResult chargeWithBillingKey(String billingKey, BigDecimal amount,
                                              String orderId, String orderName);

    /** Verifies the Toss webhook HMAC-SHA256 signature. */
    WebhookVerificationResult verifyWebhook(String signature, String rawBody);

    // ─── Result types ─────────────────────────────────────────────────────────

    record PaymentConfirmResult(boolean success, String paymentKey, String orderId,
                                BigDecimal amount, String method, String errorMessage) {}

    record PaymentCancelResult(boolean success, BigDecimal canceledAmount, String errorMessage) {}

    record BillingKeyIssueResult(boolean success, String billingKey,
                                  String maskedCardNumber, String cardCompany,
                                  String customerKey, String errorMessage) {}

    record BillingChargeResult(boolean success, String paymentKey, String orderId,
                                BigDecimal amount, String errorMessage) {}

    record WebhookVerificationResult(boolean valid, String eventType, String orderId) {}
}
