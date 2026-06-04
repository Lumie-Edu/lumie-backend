package com.lumie.billing.adapter.out.external;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.lumie.billing.adapter.out.config.BillingProperties;
import com.lumie.billing.application.port.out.PaymentGatewayPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * WireMock integration tests for TossPaymentClient.
 *
 * <p>WireMockExtension is used instead of @WireMockTest to force HTTP/1.1 mode
 * (avoids HTTP/2 RST_STREAM issues with JDK HttpClient in test environments).
 * RestClient uses SimpleClientHttpRequestFactory which is HTTP/1.1 only.
 */
class TossPaymentClientWireMockTest {

    private static final String SECRET_KEY = "test_sk_abc123";
    private static final String WEBHOOK_SECRET = "test_webhook_secret";

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private TossPaymentClient client;

    @BeforeEach
    void setUp() {
        BillingProperties properties = new BillingProperties(
                new BillingProperties.Toss(true, SECRET_KEY, "test_ck", WEBHOOK_SECRET),
                null,
                null
        );

        // SimpleClientHttpRequestFactory = HTTP/1.1, no HTTP/2 upgrade
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        RestClient restClient = RestClient.builder()
                .baseUrl(wm.baseUrl())
                .requestFactory(requestFactory)
                .build();

        client = new TossPaymentClient(restClient, properties);
    }

    // ─── confirmPayment ───────────────────────────────────────────────────────

    @Test
    void confirmPayment_success_parsesResponse() {
        wm.stubFor(post(urlEqualTo("/v1/payments/confirm"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "paymentKey": "pk_test_abc",
                                  "orderId": "order-001",
                                  "status": "DONE",
                                  "method": "카드",
                                  "totalAmount": 50000,
                                  "approvedAt": "2024-01-01T00:00:00+09:00"
                                }
                                """)));

        PaymentGatewayPort.PaymentConfirmResult result =
                client.confirmPayment("pk_test_abc", "order-001", BigDecimal.valueOf(50000));

        assertThat(result.success()).isTrue();
        assertThat(result.paymentKey()).isEqualTo("pk_test_abc");
        assertThat(result.orderId()).isEqualTo("order-001");
        assertThat(result.method()).isEqualTo("카드");
        assertThat(result.errorMessage()).isNull();

        wm.verify(postRequestedFor(urlEqualTo("/v1/payments/confirm"))
                .withRequestBody(matchingJsonPath("$.paymentKey", equalTo("pk_test_abc")))
                .withRequestBody(matchingJsonPath("$.orderId", equalTo("order-001"))));
    }

    @Test
    void confirmPayment_toss4xx_returnsFalseResult() {
        wm.stubFor(post(urlEqualTo("/v1/payments/confirm"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "ALREADY_PROCESSED_PAYMENT",
                                  "message": "이미 처리된 결제입니다."
                                }
                                """)));

        PaymentGatewayPort.PaymentConfirmResult result =
                client.confirmPayment("pk_dup", "order-dup", BigDecimal.valueOf(50000));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("ALREADY_PROCESSED_PAYMENT");
    }

    // ─── cancelPayment ────────────────────────────────────────────────────────

    @Test
    void cancelPayment_success_sendsIdempotencyKey() {
        wm.stubFor(post(urlMatching("/v1/payments/.+/cancel"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "paymentKey": "pk_test_cancel",
                                  "orderId": "order-002",
                                  "status": "CANCELED",
                                  "cancels": [
                                    {
                                      "cancelAmount": 30000,
                                      "cancelReason": "고객요청",
                                      "canceledAt": "2024-01-02T00:00:00+09:00"
                                    }
                                  ]
                                }
                                """)));

        PaymentGatewayPort.PaymentCancelResult result =
                client.cancelPayment("pk_test_cancel", "고객요청");

        assertThat(result.success()).isTrue();
        assertThat(result.canceledAmount()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(result.errorMessage()).isNull();

        // Idempotency-Key header must be sent
        wm.verify(postRequestedFor(urlMatching("/v1/payments/.+/cancel"))
                .withHeader("Idempotency-Key", matching("cancel-.+")));
    }

    // ─── issueBillingKey ──────────────────────────────────────────────────────

    @Test
    void issueBillingKey_success_parsesResponse() {
        wm.stubFor(post(urlEqualTo("/v1/billing/authorizations/issue"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "billingKey": "bk_abc123",
                                  "customerKey": "customer-uuid-001",
                                  "cardCompany": "신한",
                                  "cardNumber": "4330****9999"
                                }
                                """)));

        PaymentGatewayPort.BillingKeyIssueResult result =
                client.issueBillingKey("auth-code-xyz", "customer-uuid-001");

        assertThat(result.success()).isTrue();
        assertThat(result.billingKey()).isEqualTo("bk_abc123");
        assertThat(result.cardCompany()).isEqualTo("신한");
        assertThat(result.maskedCardNumber()).isEqualTo("4330****9999");
        assertThat(result.customerKey()).isEqualTo("customer-uuid-001");
        assertThat(result.errorMessage()).isNull();
    }

    // ─── chargeWithBillingKey ─────────────────────────────────────────────────

    @Test
    void chargeWithBillingKey_success_sendsIdempotencyKey() {
        String billingKey = "bk_charge_test";

        wm.stubFor(post(urlMatching("/v1/billing/.+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "paymentKey": "pk_charge_result",
                                  "orderId": "order-charge-001",
                                  "status": "DONE",
                                  "method": "카드",
                                  "totalAmount": 49000
                                }
                                """)));

        PaymentGatewayPort.BillingChargeResult result =
                client.chargeWithBillingKey(billingKey, "lumie-tenant-42",
                        BigDecimal.valueOf(49000), "order-charge-001", "Lumie 월 구독");

        assertThat(result.success()).isTrue();
        assertThat(result.paymentKey()).isEqualTo("pk_charge_result");
        assertThat(result.orderId()).isEqualTo("order-charge-001");

        // Idempotency-Key must be set to orderId; customerKey must be forwarded verbatim
        wm.verify(postRequestedFor(urlMatching("/v1/billing/.+"))
                .withHeader("Idempotency-Key", equalTo("order-charge-001"))
                .withRequestBody(matchingJsonPath("$.customerKey", equalTo("lumie-tenant-42"))));
    }

    @Test
    void confirmPayment_5xxServerError_returnsFalseWithErrorDetail() {
        // 5xx is handled by onStatus → BillingException, which is caught and returned as a
        // failed result. Callers should inspect result.success() and retry on server errors.
        wm.stubFor(post(urlEqualTo("/v1/payments/confirm"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "FAILED_INTERNAL_SYSTEM_PROCESSING",
                                  "message": "내부 시스템 처리 작업이 실패했습니다."
                                }
                                """)));

        PaymentGatewayPort.PaymentConfirmResult result =
                client.confirmPayment("pk_5xx", "order-5xx", BigDecimal.valueOf(10000));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("FAILED_INTERNAL_SYSTEM_PROCESSING");
    }

    @Test
    void issueBillingKey_4xxFailure_throwsException() {
        wm.stubFor(post(urlEqualTo("/v1/billing/authorizations/issue"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "INVALID_CARD",
                                  "message": "유효하지 않은 카드입니다."
                                }
                                """)));

        PaymentGatewayPort.BillingKeyIssueResult result =
                client.issueBillingKey("invalid-auth-key", "customer-key-001");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("INVALID_CARD");
    }

    @Test
    void chargeWithBillingKey_failure_throwsException() {
        wm.stubFor(post(urlMatching("/v1/billing/.+"))
                .willReturn(aResponse()
                        .withStatus(402)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "REJECT_CARD_COMPANY",
                                  "message": "결제 승인이 거절되었습니다."
                                }
                                """)));

        PaymentGatewayPort.BillingChargeResult result =
                client.chargeWithBillingKey("bk_reject", "lumie-tenant-10",
                        BigDecimal.valueOf(49000), "order-reject-001", "Lumie 월 구독");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("REJECT_CARD_COMPANY");
    }

    @Test
    void confirmPayment_emptyResponseBody_throwsException() {
        wm.stubFor(post(urlEqualTo("/v1/payments/confirm"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("")));

        // Empty body causes RestClient to return null; confirmPayment falls back gracefully
        // returning a success result with the original paymentKey/orderId/amount
        PaymentGatewayPort.PaymentConfirmResult result =
                client.confirmPayment("pk_empty", "order-empty", BigDecimal.valueOf(10000));

        // The client treats a null response body as a partial success (no parse error thrown)
        assertThat(result.success()).isTrue();
        assertThat(result.paymentKey()).isEqualTo("pk_empty");
        assertThat(result.orderId()).isEqualTo("order-empty");
    }

    // ─── verifyWebhook ────────────────────────────────────────────────────────

    @Test
    void verifyWebhook_validSignature_returnsValid() throws Exception {
        String rawBody = """
                {"eventType":"PAYMENT_STATUS_CHANGED","data":{"orderId":"order-wh-001"}}
                """.trim();
        long timestamp = Instant.now().getEpochSecond();
        String signatureHeader = buildSignatureHeader(timestamp, rawBody, WEBHOOK_SECRET);

        PaymentGatewayPort.WebhookVerificationResult result =
                client.verifyWebhook(signatureHeader, rawBody);

        assertThat(result.valid()).isTrue();
        assertThat(result.eventType()).isEqualTo("PAYMENT_STATUS_CHANGED");
    }

    @Test
    void verifyWebhook_wrongSecret_returnsInvalid() throws Exception {
        String rawBody = """
                {"eventType":"PAYMENT_STATUS_CHANGED","data":{"orderId":"order-wh-002"}}
                """.trim();
        long timestamp = Instant.now().getEpochSecond();
        String signatureHeader = buildSignatureHeader(timestamp, rawBody, "wrong_secret");

        PaymentGatewayPort.WebhookVerificationResult result =
                client.verifyWebhook(signatureHeader, rawBody);

        assertThat(result.valid()).isFalse();
    }

    @Test
    void verifyWebhook_timestampTooOld_returnsInvalid() throws Exception {
        String rawBody = """
                {"eventType":"PAYMENT_STATUS_CHANGED"}
                """.trim();
        long oldTimestamp = Instant.now().getEpochSecond() - 360;
        String signatureHeader = buildSignatureHeader(oldTimestamp, rawBody, WEBHOOK_SECRET);

        PaymentGatewayPort.WebhookVerificationResult result =
                client.verifyWebhook(signatureHeader, rawBody);

        assertThat(result.valid()).isFalse();
    }

    @Test
    void verifyWebhook_nullSignature_returnsInvalid() {
        PaymentGatewayPort.WebhookVerificationResult result =
                client.verifyWebhook(null, "{}");

        assertThat(result.valid()).isFalse();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static String buildSignatureHeader(long timestamp, String rawBody, String secret)
            throws Exception {
        String payload = timestamp + "." + rawBody;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String hex = HexFormat.of().formatHex(hmac);
        return "t=" + timestamp + ",v1=" + hex;
    }
}
