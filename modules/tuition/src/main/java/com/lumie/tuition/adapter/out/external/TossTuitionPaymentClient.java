package com.lumie.tuition.adapter.out.external;

import com.lumie.tuition.adapter.out.config.TuitionModuleConfig;
import com.lumie.tuition.application.port.out.TuitionPaymentGatewayPort;
import com.lumie.tuition.domain.entity.MerchantProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stub implementation of TuitionPaymentGatewayPort (Toss Payments).
 *
 * TODO (Task 5 PoC): implement actual Toss Payments integration.
 * When implementing:
 * 1. Add to Vault: secret/applications/toss-tuition (secretKey, clientKey, webhookSecret, subMallId)
 * 2. Update common-values.yaml with VaultStaticSecret for toss-tuition credentials
 * 3. Inject TuitionModuleConfig.toss.secretKey as Base64 for HTTP Basic auth
 * 4. Toss confirm endpoint: POST https://api.tosspayments.com/v1/payments/confirm
 * 5. Toss refund endpoint: POST https://api.tosspayments.com/v1/payments/{paymentKey}/cancel
 * 6. Toss sub-mall seller registration: POST https://api.tosspayments.com/v1/brandpay/sellers
 *
 * NOTE: All calls must be made OUTSIDE @Transactional boundaries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TossTuitionPaymentClient implements TuitionPaymentGatewayPort {

    private final TuitionModuleConfig config;

    @Override
    public PaymentRequestResult requestPayment(TuitionPaymentRequest req) {
        log.warn("STUB: TossTuitionPaymentClient.requestPayment not implemented. orderId={}", req.orderId());
        // TODO: generate checkout URL via Toss Payment Widget API
        return new PaymentRequestResult(
                true,
                "https://sandbox-pay.toss.im/stub?orderId=" + req.orderId(),
                null,
                "STUB: Toss tuition payment integration pending"
        );
    }

    @Override
    public PaymentCaptureResult confirmPayment(String paymentKey, String orderId, BigDecimal amount) {
        log.warn("STUB: TossTuitionPaymentClient.confirmPayment not implemented. orderId={}", orderId);
        // TODO: POST /v1/payments/confirm with Basic Auth
        return new PaymentCaptureResult(
                true,
                paymentKey,
                paymentKey,
                orderId,
                amount,
                "CARD",
                null,
                "STUB: Toss tuition payment integration pending"
        );
    }

    @Override
    public RefundResult refund(String paymentKey, BigDecimal amount, String reason, String idempotencyKey) {
        log.warn("STUB: TossTuitionPaymentClient.refund not implemented. paymentKey={}", paymentKey);
        // TODO: POST /v1/payments/{paymentKey}/cancel
        return new RefundResult(true, amount, null, "STUB: Toss tuition refund integration pending");
    }

    @Override
    public SellerRegistrationResult registerSeller(MerchantProfile profile, KycDocuments documents) {
        log.warn("STUB: TossTuitionPaymentClient.registerSeller not implemented. profileId={}", profile.getId());
        // TODO: POST /v1/brandpay/sellers (sub-mall seller registration)
        return new SellerRegistrationResult(
                false,
                null,
                "STUB_NOT_IMPLEMENTED",
                "STUB: Toss seller registration integration pending"
        );
    }

    @Override
    public WebhookVerificationResult verifyWebhook(String signature, String rawBody) {
        log.warn("STUB: TossTuitionPaymentClient.verifyWebhook — accepting all signatures in stub mode");
        // TODO: implement HMAC-SHA256 verification with toss.webhookSecret
        // Algorithm: HMAC-SHA256(key=webhookSecret, message=rawBody) → compare with signature header
        return new WebhookVerificationResult(true, "PAYMENT_STATUS_CHANGED", "stub-order-id");
    }
}
