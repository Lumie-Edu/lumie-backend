package com.lumie.tuition.application.port.out;

import com.lumie.tuition.domain.entity.MerchantProfile;

import java.math.BigDecimal;

/**
 * Outbound port for Toss Payments integration.
 * Implementations MUST be called OUTSIDE @Transactional boundaries.
 * See HARD_RULES_CROSS: "Wrap external HTTP calls OUTSIDE @Transactional boundaries."
 */
public interface TuitionPaymentGatewayPort {

    /**
     * Creates a payment widget session (token + redirect URL) for the given request.
     * Called before the transaction is opened — the caller records a PENDING TuitionPayment
     * first, then commits, then calls this method.
     */
    PaymentRequestResult requestPayment(TuitionPaymentRequest req);

    /**
     * Confirms (captures) a payment with Toss after the user completes the widget flow.
     */
    PaymentCaptureResult confirmPayment(String paymentKey, String orderId, BigDecimal amount);

    /**
     * Issues a full or partial refund for a previously captured payment.
     */
    RefundResult refund(String paymentKey, BigDecimal amount, String reason, String idempotencyKey);

    /**
     * Registers the academy as a sub-merchant (marketplace seller) with Toss.
     */
    SellerRegistrationResult registerSeller(MerchantProfile profile, KycDocuments documents);

    /**
     * Verifies the HMAC-SHA256 signature on an incoming Toss webhook request.
     */
    WebhookVerificationResult verifyWebhook(String signature, String rawBody);

    // ─── Result records ───────────────────────────────────────────────────────

    record TuitionPaymentRequest(
            String orderId,
            String orderName,
            BigDecimal amount,
            String customerName,
            String customerEmail,
            String successUrl,
            String failUrl
    ) {}

    record PaymentRequestResult(
            boolean success,
            String checkoutUrl,
            String errorCode,
            String errorMessage
    ) {}

    record PaymentCaptureResult(
            boolean success,
            String pgTransactionId,
            String paymentKey,
            String orderId,
            BigDecimal approvedAmount,
            String method,
            String errorCode,
            String errorMessage
    ) {}

    record RefundResult(
            boolean success,
            BigDecimal refundedAmount,
            String errorCode,
            String errorMessage
    ) {}

    record KycDocuments(
            String businessRegistrationUrl,
            String bankbookUrl
    ) {}

    record SellerRegistrationResult(
            boolean success,
            String pgSellerKey,
            String errorCode,
            String errorMessage
    ) {}

    record WebhookVerificationResult(
            boolean valid,
            String eventType,
            String orderId
    ) {}
}
