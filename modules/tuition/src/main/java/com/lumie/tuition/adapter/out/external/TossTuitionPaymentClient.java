package com.lumie.tuition.adapter.out.external;

import com.lumie.tuition.adapter.out.config.TuitionProperties;
import com.lumie.tuition.application.dto.external.toss.TossCancelResponse;
import com.lumie.tuition.application.dto.external.toss.TossPaymentResponse;
import com.lumie.tuition.application.port.out.TuitionPaymentGatewayPort;
import com.lumie.tuition.domain.entity.MerchantProfile;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * Live Toss Payments integration for tuition payments.
 *
 * <p>All methods MUST be called OUTSIDE {@code @Transactional} boundaries
 * (HARD_RULES_CROSS: "Wrap external HTTP calls OUTSIDE @Transactional boundaries").
 *
 * <p>HTTP 4xx/5xx responses are converted to {@link TuitionException}(TOSS_API_ERROR)
 * with the Toss {@code code} and {@code message} embedded in the exception message.
 *
 * <p>{@code requestPayment} — the Toss Payment Widget flow is entirely client-side:
 * the frontend SDK opens the payment widget, the user completes payment, and the browser
 * calls the success callback with {@code paymentKey}. The backend only validates input
 * and returns {@code orderId}/{@code amount} for the frontend to begin the widget session.
 * No server-to-server call is needed at this step; only {@code confirmPayment} calls Toss.
 */
@Slf4j
@Component
public class TossTuitionPaymentClient implements TuitionPaymentGatewayPort {

    private static final int WEBHOOK_MAX_AGE_SECONDS = 300;

    private final RestClient restClient;
    private final TuitionProperties properties;

    public TossTuitionPaymentClient(
            @Qualifier("tossPaymentsRestClient") RestClient restClient,
            TuitionProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    // ─── TuitionPaymentGatewayPort ────────────────────────────────────────────

    /**
     * Validates the payment request parameters and returns them to the caller so
     * the frontend can initialize the Toss Payment Widget.
     * No server-to-server PG call is made here — the widget flow is entirely client-side.
     */
    @Override
    public PaymentRequestResult requestPayment(TuitionPaymentRequest req) {
        log.info("TossTuitionPaymentClient.requestPayment: orderId={}, amount={}", req.orderId(), req.amount());

        if (req.orderId() == null || req.orderId().isBlank()) {
            return new PaymentRequestResult(false, null, "INVALID_ORDER_ID", "orderId is required");
        }
        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return new PaymentRequestResult(false, null, "INVALID_AMOUNT", "amount must be positive");
        }

        // Return validation-only result; frontend will use orderId + amount for widget init
        return new PaymentRequestResult(true, null, null, null);
    }

    /**
     * Confirms (captures) a payment after the user completes the Toss widget flow.
     * POST /v1/payments/confirm
     */
    @Override
    public PaymentCaptureResult confirmPayment(String paymentKey, String orderId, BigDecimal amount) {
        log.info("TossTuitionPaymentClient.confirmPayment: orderId={}", orderId);

        Map<String, Object> requestBody = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderId,
                "amount", amount
        );

        try {
            TossPaymentResponse response = restClient.post()
                    .uri("/v1/payments/confirm")
                    .header("Authorization", basicAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(), (req, res) -> {
                        throw tossApiException(res, "confirmPayment");
                    })
                    .body(TossPaymentResponse.class);

            log.info("TossTuitionPaymentClient.confirmPayment success: orderId={}", orderId);

            return new PaymentCaptureResult(
                    true,
                    response != null ? response.paymentKey() : paymentKey,
                    response != null ? response.paymentKey() : paymentKey,
                    response != null ? response.orderId() : orderId,
                    response != null ? response.totalAmount() : amount,
                    response != null ? response.method() : null,
                    null,
                    null
            );

        } catch (TuitionException e) {
            log.error("TossTuitionPaymentClient.confirmPayment failed: orderId={}, error={}", orderId, e.getMessage());
            return new PaymentCaptureResult(false, null, paymentKey, orderId, amount, null,
                    e.getErrorCode().getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("TossTuitionPaymentClient.confirmPayment unexpected error: orderId={}", orderId, e);
            throw new TuitionException(TuitionErrorCode.TOSS_API_ERROR,
                    "Unexpected error during confirmPayment: " + e.getMessage());
        }
    }

    /**
     * Issues a full or partial refund.
     * POST /v1/payments/{paymentKey}/cancel
     * Idempotency-Key is required by Toss for cancel requests.
     *
     * <p>If {@code idempotencyKey} is null, a stable fallback of {@code "refund-" + paymentKey}
     * is used. Because one paymentKey can have multiple partial refunds, callers performing
     * partial refunds MUST supply a caller-generated idempotencyKey that encodes the amount
     * (e.g. {@code "refund-" + paymentKey + "-" + amount.toPlainString()}) to prevent
     * idempotency collisions between distinct partial refund requests.
     */
    @Override
    public RefundResult refund(String paymentKey, BigDecimal amount, String reason, String idempotencyKey) {
        log.info("TossTuitionPaymentClient.refund: paymentKey={}, amount={}", paymentKey, amount);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("cancelReason", reason);
        if (amount != null) {
            requestBody.put("cancelAmount", amount);
        }

        // paymentKey is the unique PG identifier — stable fallback guarantees safe full-refund retries.
        // Partial-refund callers must supply an explicit key (see Javadoc above).
        String effectiveIdempotencyKey = idempotencyKey != null ? idempotencyKey
                : "refund-" + paymentKey;

        try {
            TossCancelResponse response = restClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .header("Authorization", basicAuthHeader())
                    .header("Idempotency-Key", effectiveIdempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(), (req, res) -> {
                        throw tossApiException(res, "refund");
                    })
                    .body(TossCancelResponse.class);

            BigDecimal refundedAmount = BigDecimal.ZERO;
            if (response != null && response.cancels() != null && !response.cancels().isEmpty()) {
                refundedAmount = response.cancels().stream()
                        .map(TossCancelResponse.CancelDetail::cancelAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else if (amount != null) {
                refundedAmount = amount;
            }

            log.info("TossTuitionPaymentClient.refund success: paymentKey={}, refundedAmount={}",
                    paymentKey, refundedAmount);

            return new RefundResult(true, refundedAmount, null, null);

        } catch (TuitionException e) {
            log.error("TossTuitionPaymentClient.refund failed: paymentKey={}, error={}", paymentKey, e.getMessage());
            return new RefundResult(false, BigDecimal.ZERO, e.getErrorCode().getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("TossTuitionPaymentClient.refund unexpected error: paymentKey={}", paymentKey, e);
            throw new TuitionException(TuitionErrorCode.TOSS_API_ERROR,
                    "Unexpected error during refund: " + e.getMessage());
        }
    }

    /**
     * Sub-merchant seller registration (KYC).
     *
     * <p>Intentionally kept as stub. Toss sub-mall seller registration requires:
     * 1. Manual KYC approval process with Toss Payments business team.
     * 2. A different API contract (brandpay/sellers) that behaves differently in Sandbox.
     * 3. Integration with Toss's document submission workflow.
     *
     * TODO: Implement when Toss sub-mall agreement is signed and sandbox KYC endpoint is validated.
     * See Toss docs: https://docs.tosspayments.com/reference/brandpay
     */
    @Override
    public SellerRegistrationResult registerSeller(MerchantProfile profile, KycDocuments documents) {
        log.warn("TossTuitionPaymentClient.registerSeller: STUB — KYC seller registration not yet implemented. profileId={}",
                profile.getId());
        return new SellerRegistrationResult(
                false,
                null,
                "NOT_IMPLEMENTED",
                "Toss sub-mall seller registration is pending KYC agreement with Toss Payments. " +
                        "Contact support to enable this feature."
        );
    }

    /**
     * Verifies the Toss webhook HMAC-SHA256 signature.
     *
     * <p>Signature header format: {@code t=<timestamp>,v1=<hex(HMAC-SHA256)>}
     * Payload for HMAC: {@code <timestamp>.<rawBody>}
     * Replay protection: timestamp must be within 5 minutes of now.
     *
     * <p>{@link MessageDigest#isEqual} is used for constant-time byte comparison to prevent
     * timing side-channel attacks.
     */
    @Override
    public WebhookVerificationResult verifyWebhook(String signature, String rawBody) {
        if (signature == null || signature.isBlank()) {
            log.warn("TossTuitionPaymentClient.verifyWebhook: missing signature header");
            return new WebhookVerificationResult(false, null, null);
        }

        try {
            // split with limit=2 avoids Guava Splitter dependency while satisfying linter
            String[] parts = signature.split(",", 2);
            if (parts.length < 2) {
                log.warn("TossTuitionPaymentClient.verifyWebhook: malformed signature header");
                return new WebhookVerificationResult(false, null, null);
            }

            long timestamp = Long.parseLong(parts[0].substring(2));
            String expectedHex = parts[1].substring(3);

            long nowSeconds = Instant.now().getEpochSecond();
            if (Math.abs(nowSeconds - timestamp) > WEBHOOK_MAX_AGE_SECONDS) {
                log.warn("TossTuitionPaymentClient.verifyWebhook: timestamp too old or in future. diff={}s",
                        Math.abs(nowSeconds - timestamp));
                return new WebhookVerificationResult(false, null, null);
            }

            String webhookSecret = properties.toss().webhookSecret();
            if (webhookSecret == null || webhookSecret.isBlank()) {
                log.error("TossTuitionPaymentClient.verifyWebhook: webhookSecret not configured");
                return new WebhookVerificationResult(false, null, null);
            }

            // Parse the expected HMAC bytes from hex. Fails fast with IllegalArgumentException
            // if the header contains a malformed hex string (treats it as verification failure).
            byte[] expectedBytes;
            try {
                expectedBytes = HexFormat.of().parseHex(expectedHex);
            } catch (IllegalArgumentException e) {
                log.warn("TossTuitionPaymentClient.verifyWebhook: expectedHex is not valid hex");
                return new WebhookVerificationResult(false, null, null);
            }

            String payload = timestamp + "." + rawBody;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] actualBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Constant-time comparison on raw HMAC bytes (not hex strings) to prevent timing attacks
            boolean valid = MessageDigest.isEqual(expectedBytes, actualBytes);

            if (!valid) {
                log.warn("TossTuitionPaymentClient.verifyWebhook: HMAC mismatch");
                return new WebhookVerificationResult(false, null, null);
            }

            String eventType = extractJsonField(rawBody, "eventType");
            String orderId = extractJsonField(rawBody, "orderId");

            log.debug("TossTuitionPaymentClient.verifyWebhook: valid. eventType={}", eventType);
            return new WebhookVerificationResult(true, eventType, orderId);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("TossTuitionPaymentClient.verifyWebhook: HMAC computation failed", e);
            throw new TuitionException(TuitionErrorCode.INTERNAL_ERROR,
                    "Webhook HMAC computation failed: " + e.getMessage());
        } catch (Exception e) {
            log.warn("TossTuitionPaymentClient.verifyWebhook: unexpected error", e);
            return new WebhookVerificationResult(false, null, null);
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String basicAuthHeader() {
        String secretKey = properties.toss().secretKey();
        if (secretKey == null || secretKey.isBlank()) {
            throw new TuitionException(TuitionErrorCode.INTERNAL_ERROR,
                    "Toss secretKey is not configured");
        }
        String credentials = secretKey + ":";
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private TuitionException tossApiException(
            org.springframework.http.client.ClientHttpResponse res, String operation) {
        try {
            int statusCode = res.getStatusCode().value();
            byte[] bodyBytes = res.getBody().readAllBytes();
            String bodyStr = new String(bodyBytes, StandardCharsets.UTF_8);

            String tossCode = extractJsonField(bodyStr, "code");
            String tossMessage = extractJsonField(bodyStr, "message");

            log.error("Toss API error in {}: status={}, code={}, message={}",
                    operation, statusCode, tossCode, tossMessage);

            String detail = "[%s] %s (HTTP %d)".formatted(
                    tossCode != null ? tossCode : "UNKNOWN",
                    tossMessage != null ? tossMessage : bodyStr,
                    statusCode);

            return new TuitionException(TuitionErrorCode.TOSS_API_ERROR, detail);
        } catch (Exception e) {
            log.error("Failed to parse Toss error response in {}", operation, e);
            return new TuitionException(TuitionErrorCode.TOSS_API_ERROR,
                    "Toss API call failed in " + operation);
        }
    }

    private static String extractJsonField(String json, String fieldName) {
        if (json == null) return null;
        String search = "\"" + fieldName + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf('"', start);
        return end > start ? json.substring(start, end) : null;
    }
}
