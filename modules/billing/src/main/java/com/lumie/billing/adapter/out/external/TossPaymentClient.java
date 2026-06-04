package com.lumie.billing.adapter.out.external;

import com.lumie.billing.adapter.out.config.BillingProperties;
import com.lumie.billing.application.dto.external.toss.TossBillingKeyResponse;
import com.lumie.billing.application.dto.external.toss.TossCancelResponse;
import com.lumie.billing.application.dto.external.toss.TossPaymentResponse;
import com.lumie.billing.application.port.out.PaymentGatewayPort;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
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
import java.util.HexFormat;
import java.util.Map;

/**
 * Live Toss Payments integration for subscription billing.
 *
 * <p>All methods in this class MUST be called OUTSIDE {@code @Transactional} boundaries
 * (see HARD_RULES_CROSS: "Wrap external HTTP calls OUTSIDE @Transactional boundaries").
 *
 * <p>HTTP 4xx/5xx responses are converted to {@link BillingException}(TOSS_API_ERROR)
 * with the Toss {@code code} and {@code message} fields embedded in the exception message.
 * Callers receive result records; exceptions surface payment failures to upper layers.
 *
 * <p>Audit logging (PaymentTransactionLogger.REQUEST/RESPONSE) is intentionally done
 * at the service layer (PaymentService, BillingKeyCommandService, MonthlySubscriptionChargeScheduler)
 * to keep the adapter thin and consistent with the existing codebase pattern.
 */
@Slf4j
@Component
public class TossPaymentClient implements PaymentGatewayPort {

    private static final int WEBHOOK_MAX_AGE_SECONDS = 300;

    private final RestClient restClient;
    private final BillingProperties properties;

    public TossPaymentClient(
            @Qualifier("tossBillingRestClient") RestClient restClient,
            BillingProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    // ─── PaymentGatewayPort ───────────────────────────────────────────────────

    /**
     * Confirms a one-time payment.
     * POST /v1/payments/confirm
     */
    @Override
    public PaymentConfirmResult confirmPayment(String paymentKey, String orderId, BigDecimal amount) {
        log.info("TossPaymentClient.confirmPayment: orderId={}", orderId);

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

            log.info("TossPaymentClient.confirmPayment success: orderId={}, status={}",
                    orderId, response != null ? response.status() : "null");

            return new PaymentConfirmResult(
                    true,
                    response != null ? response.paymentKey() : paymentKey,
                    response != null ? response.orderId() : orderId,
                    response != null ? response.totalAmount() : amount,
                    response != null ? response.method() : null,
                    null
            );

        } catch (BillingException e) {
            log.error("TossPaymentClient.confirmPayment failed: orderId={}, error={}", orderId, e.getMessage());
            return new PaymentConfirmResult(false, paymentKey, orderId, amount, null, e.getMessage());
        } catch (Exception e) {
            log.error("TossPaymentClient.confirmPayment unexpected error: orderId={}", orderId, e);
            throw new BillingException(BillingErrorCode.TOSS_API_ERROR,
                    "Unexpected error during confirmPayment: " + e.getMessage());
        }
    }

    /**
     * Cancels (full refund) a payment.
     * POST /v1/payments/{paymentKey}/cancel
     * Idempotency-Key is derived from the paymentKey to ensure safe retries.
     */
    @Override
    public PaymentCancelResult cancelPayment(String paymentKey, String reason) {
        log.info("TossPaymentClient.cancelPayment: paymentKey={}", paymentKey);

        Map<String, Object> requestBody = Map.of("cancelReason", reason);
        String idempotencyKey = "cancel-" + paymentKey;

        try {
            TossCancelResponse response = restClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .header("Authorization", basicAuthHeader())
                    .header("Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(), (req, res) -> {
                        throw tossApiException(res, "cancelPayment");
                    })
                    .body(TossCancelResponse.class);

            BigDecimal canceledAmount = BigDecimal.ZERO;
            if (response != null && response.cancels() != null && !response.cancels().isEmpty()) {
                canceledAmount = response.cancels().stream()
                        .map(TossCancelResponse.CancelDetail::cancelAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            log.info("TossPaymentClient.cancelPayment success: paymentKey={}, canceledAmount={}",
                    paymentKey, canceledAmount);

            return new PaymentCancelResult(true, canceledAmount, null);

        } catch (BillingException e) {
            log.error("TossPaymentClient.cancelPayment failed: paymentKey={}, error={}", paymentKey, e.getMessage());
            return new PaymentCancelResult(false, BigDecimal.ZERO, e.getMessage());
        } catch (Exception e) {
            log.error("TossPaymentClient.cancelPayment unexpected error: paymentKey={}", paymentKey, e);
            throw new BillingException(BillingErrorCode.TOSS_API_ERROR,
                    "Unexpected error during cancelPayment: " + e.getMessage());
        }
    }

    /**
     * Issues a Toss billing key from a one-time auth code.
     * POST /v1/billing/authorizations/issue
     */
    @Override
    public BillingKeyIssueResult issueBillingKey(String authKey, String customerKey) {
        log.info("TossPaymentClient.issueBillingKey: customerKey={}", customerKey);

        Map<String, Object> requestBody = Map.of(
                "authKey", authKey,
                "customerKey", customerKey
        );

        try {
            TossBillingKeyResponse response = restClient.post()
                    .uri("/v1/billing/authorizations/issue")
                    .header("Authorization", basicAuthHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(), (req, res) -> {
                        throw tossApiException(res, "issueBillingKey");
                    })
                    .body(TossBillingKeyResponse.class);

            log.info("TossPaymentClient.issueBillingKey success: customerKey={}", customerKey);

            return new BillingKeyIssueResult(
                    true,
                    response != null ? response.billingKey() : null,
                    response != null ? response.cardNumber() : null,
                    response != null ? response.cardCompany() : null,
                    customerKey,
                    null
            );

        } catch (BillingException e) {
            log.error("TossPaymentClient.issueBillingKey failed: customerKey={}, error={}", customerKey, e.getMessage());
            return new BillingKeyIssueResult(false, null, null, null, customerKey, e.getMessage());
        } catch (Exception e) {
            log.error("TossPaymentClient.issueBillingKey unexpected error: customerKey={}", customerKey, e);
            throw new BillingException(BillingErrorCode.TOSS_API_ERROR,
                    "Unexpected error during issueBillingKey: " + e.getMessage());
        }
    }

    /**
     * Charges a registered billing key (automatic monthly subscription billing).
     * POST /v1/billing/{billingKey}
     * Idempotency-Key is set to the orderId for safe retries.
     *
     * @param customerKey must be the exact customerKey used at billing key issuance
     *                    (stored in {@code BillingKey.customerKey}). Toss will reject
     *                    the charge with NOT_FOUND_CUSTOMER_KEY if this does not match.
     */
    @Override
    public BillingChargeResult chargeWithBillingKey(String billingKey, String customerKey,
                                                     BigDecimal amount, String orderId,
                                                     String orderName) {
        log.info("TossPaymentClient.chargeWithBillingKey: orderId={}, amount={}", orderId, amount);

        Map<String, Object> requestBody = Map.of(
                "customerKey", customerKey,
                "amount", amount,
                "orderId", orderId,
                "orderName", orderName
        );

        try {
            TossPaymentResponse response = restClient.post()
                    .uri("/v1/billing/{billingKey}", billingKey)
                    .header("Authorization", basicAuthHeader())
                    .header("Idempotency-Key", orderId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(code -> code.is4xxClientError() || code.is5xxServerError(), (req, res) -> {
                        throw tossApiException(res, "chargeWithBillingKey");
                    })
                    .body(TossPaymentResponse.class);

            log.info("TossPaymentClient.chargeWithBillingKey success: orderId={}", orderId);

            return new BillingChargeResult(
                    true,
                    response != null ? response.paymentKey() : null,
                    orderId,
                    amount,
                    null
            );

        } catch (BillingException e) {
            log.error("TossPaymentClient.chargeWithBillingKey failed: orderId={}, error={}", orderId, e.getMessage());
            return new BillingChargeResult(false, null, orderId, amount, e.getMessage());
        } catch (Exception e) {
            log.error("TossPaymentClient.chargeWithBillingKey unexpected error: orderId={}", orderId, e);
            throw new BillingException(BillingErrorCode.TOSS_API_ERROR,
                    "Unexpected error during chargeWithBillingKey: " + e.getMessage());
        }
    }

    /**
     * Verifies the Toss webhook HMAC-SHA256 signature.
     *
     * <p>Signature header format: {@code t=<timestamp>,v1=<hex(HMAC-SHA256)>}
     * Payload for HMAC: {@code <timestamp>.<rawBody>}
     * Replay protection: timestamp must be within 5 minutes of now.
     *
     * <p>{@link MessageDigest#isEqual} is used for constant-time comparison to prevent
     * timing attacks where a naive string comparison could leak information about
     * how many bytes match.
     */
    @Override
    public WebhookVerificationResult verifyWebhook(String signature, String rawBody) {
        if (signature == null || signature.isBlank()) {
            log.warn("TossPaymentClient.verifyWebhook: missing signature header");
            return new WebhookVerificationResult(false, null, null);
        }

        try {
            // split with limit=2 avoids Guava Splitter dependency while satisfying linter
            String[] parts = signature.split(",", 2);
            if (parts.length < 2) {
                log.warn("TossPaymentClient.verifyWebhook: malformed signature header");
                return new WebhookVerificationResult(false, null, null);
            }

            long timestamp = Long.parseLong(parts[0].substring(2));
            String expectedHex = parts[1].substring(3);

            long nowSeconds = Instant.now().getEpochSecond();
            if (Math.abs(nowSeconds - timestamp) > WEBHOOK_MAX_AGE_SECONDS) {
                log.warn("TossPaymentClient.verifyWebhook: timestamp too old or in future. diff={}s",
                        Math.abs(nowSeconds - timestamp));
                return new WebhookVerificationResult(false, null, null);
            }

            String webhookSecret = properties.toss().webhookSecret();
            if (webhookSecret == null || webhookSecret.isBlank()) {
                log.error("TossPaymentClient.verifyWebhook: webhookSecret not configured");
                return new WebhookVerificationResult(false, null, null);
            }

            // Parse the expected HMAC bytes from hex. Fails fast with IllegalArgumentException
            // if the header contains a malformed hex string (treats it as verification failure).
            byte[] expectedBytes;
            try {
                expectedBytes = HexFormat.of().parseHex(expectedHex);
            } catch (IllegalArgumentException e) {
                log.warn("TossPaymentClient.verifyWebhook: expectedHex is not valid hex");
                return new WebhookVerificationResult(false, null, null);
            }

            String payload = timestamp + "." + rawBody;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] actualBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Constant-time comparison on raw HMAC bytes (not hex strings) to prevent timing attacks
            boolean valid = MessageDigest.isEqual(expectedBytes, actualBytes);

            if (!valid) {
                log.warn("TossPaymentClient.verifyWebhook: HMAC mismatch");
                return new WebhookVerificationResult(false, null, null);
            }

            // Extract eventType and orderId from the raw body for the result record.
            // Simple substring extraction avoids a full JSON parse at this layer.
            String eventType = extractJsonField(rawBody, "eventType");
            String orderId = extractJsonField(rawBody, "orderId");

            log.debug("TossPaymentClient.verifyWebhook: valid. eventType={}", eventType);
            return new WebhookVerificationResult(true, eventType, orderId);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("TossPaymentClient.verifyWebhook: HMAC computation failed", e);
            throw new BillingException(BillingErrorCode.INTERNAL_ERROR,
                    "Webhook HMAC computation failed: " + e.getMessage());
        } catch (Exception e) {
            log.warn("TossPaymentClient.verifyWebhook: unexpected error", e);
            return new WebhookVerificationResult(false, null, null);
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String basicAuthHeader() {
        String secretKey = properties.toss().secretKey();
        if (secretKey == null || secretKey.isBlank()) {
            throw new BillingException(BillingErrorCode.INTERNAL_ERROR,
                    "Toss secretKey is not configured");
        }
        String credentials = secretKey + ":";
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Reads the Toss error body and throws a {@link BillingException} with the PG's own
     * error code and message embedded, so callers get actionable detail in logs.
     */
    private BillingException tossApiException(
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

            return new BillingException(BillingErrorCode.TOSS_API_ERROR, detail);
        } catch (Exception e) {
            log.error("Failed to parse Toss error response in {}", operation, e);
            return new BillingException(BillingErrorCode.TOSS_API_ERROR,
                    "Toss API call failed in " + operation);
        }
    }

    /**
     * Extracts a simple top-level JSON string field without a full Jackson parse.
     * Sufficient for flat Toss error/event payloads; does not handle nested objects.
     */
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
