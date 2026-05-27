package com.lumie.tuition.adapter.out.external;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.lumie.tuition.adapter.out.config.TuitionProperties;
import com.lumie.tuition.application.port.out.TuitionPaymentGatewayPort;
import com.lumie.tuition.domain.exception.TuitionException;
import org.junit.jupiter.api.Assertions;
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
 * WireMock integration tests for TossTuitionPaymentClient.
 *
 * <p>WireMockExtension (not @WireMockTest) forces HTTP/1.1 to avoid RST_STREAM
 * issues with JDK HttpClient in test environments.
 * RestClient uses SimpleClientHttpRequestFactory (HTTP/1.1 only).
 */
class TossTuitionPaymentClientWireMockTest {

    private static final String SECRET_KEY = "test_tuition_sk";
    private static final String WEBHOOK_SECRET = "test_tuition_webhook_secret";

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private TossTuitionPaymentClient client;

    @BeforeEach
    void setUp() {
        TuitionProperties properties = new TuitionProperties(
                new TuitionProperties.Toss(true, SECRET_KEY, "test_ck", WEBHOOK_SECRET)
        );

        // SimpleClientHttpRequestFactory = HTTP/1.1, no HTTP/2 upgrade
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        RestClient restClient = RestClient.builder()
                .baseUrl(wm.baseUrl())
                .requestFactory(requestFactory)
                .build();

        client = new TossTuitionPaymentClient(restClient, properties);
    }

    // ─── requestPayment ───────────────────────────────────────────────────────

    @Test
    void requestPayment_validInput_returnsSuccess() {
        TuitionPaymentGatewayPort.TuitionPaymentRequest req = new TuitionPaymentGatewayPort.TuitionPaymentRequest(
                "order-tuition-001", "Lumie 수강료", BigDecimal.valueOf(300000),
                "홍길동", "hong@example.com",
                "https://example.com/success", "https://example.com/fail"
        );

        TuitionPaymentGatewayPort.PaymentRequestResult result = client.requestPayment(req);

        assertThat(result.success()).isTrue();
        assertThat(result.errorCode()).isNull();
    }

    @Test
    void requestPayment_invalidAmount_returnsFailure() {
        TuitionPaymentGatewayPort.TuitionPaymentRequest req = new TuitionPaymentGatewayPort.TuitionPaymentRequest(
                "order-bad", "Test", BigDecimal.ZERO,
                "홍길동", "hong@example.com", null, null
        );

        TuitionPaymentGatewayPort.PaymentRequestResult result = client.requestPayment(req);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("INVALID_AMOUNT");
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
                                  "paymentKey": "pk_tuition_abc",
                                  "orderId": "order-tuition-001",
                                  "status": "DONE",
                                  "method": "카드",
                                  "totalAmount": 300000,
                                  "approvedAt": "2024-01-01T00:00:00+09:00"
                                }
                                """)));

        TuitionPaymentGatewayPort.PaymentCaptureResult result =
                client.confirmPayment("pk_tuition_abc", "order-tuition-001", BigDecimal.valueOf(300000));

        assertThat(result.success()).isTrue();
        assertThat(result.paymentKey()).isEqualTo("pk_tuition_abc");
        assertThat(result.orderId()).isEqualTo("order-tuition-001");
        assertThat(result.approvedAmount()).isEqualByComparingTo(BigDecimal.valueOf(300000));
        assertThat(result.errorMessage()).isNull();

        wm.verify(postRequestedFor(urlEqualTo("/v1/payments/confirm"))
                .withRequestBody(matchingJsonPath("$.paymentKey", equalTo("pk_tuition_abc")))
                .withRequestBody(matchingJsonPath("$.orderId", equalTo("order-tuition-001"))));
    }

    @Test
    void confirmPayment_toss4xx_returnsFalseResult() {
        wm.stubFor(post(urlEqualTo("/v1/payments/confirm"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "PAY_PROCESS_ABORTED",
                                  "message": "결제가 취소되었습니다."
                                }
                                """)));

        TuitionPaymentGatewayPort.PaymentCaptureResult result =
                client.confirmPayment("pk_aborted", "order-aborted", BigDecimal.valueOf(100000));

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isNotNull();
        assertThat(result.errorMessage()).contains("PAY_PROCESS_ABORTED");
    }

    // ─── refund ───────────────────────────────────────────────────────────────

    @Test
    void refund_success_sendsIdempotencyKeyHeader() {
        wm.stubFor(post(urlMatching("/v1/payments/.+/cancel"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "paymentKey": "pk_refund_test",
                                  "orderId": "order-refund-001",
                                  "status": "PARTIAL_CANCELED",
                                  "cancels": [
                                    {
                                      "cancelAmount": 50000,
                                      "cancelReason": "학원 사정으로 인한 환불",
                                      "canceledAt": "2024-01-03T00:00:00+09:00"
                                    }
                                  ]
                                }
                                """)));

        TuitionPaymentGatewayPort.RefundResult result =
                client.refund("pk_refund_test", BigDecimal.valueOf(50000),
                        "학원 사정으로 인한 환불", "idem-key-001");

        assertThat(result.success()).isTrue();
        assertThat(result.refundedAmount()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(result.errorMessage()).isNull();

        // Explicit Idempotency-Key must be forwarded
        wm.verify(postRequestedFor(urlMatching("/v1/payments/.+/cancel"))
                .withHeader("Idempotency-Key", equalTo("idem-key-001")));
    }

    @Test
    void refund_nullIdempotencyKey_generatesOne() {
        wm.stubFor(post(urlMatching("/v1/payments/.+/cancel"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "paymentKey": "pk_refund_auto",
                                  "orderId": "order-refund-auto",
                                  "status": "CANCELED",
                                  "cancels": [
                                    {"cancelAmount": 100000, "cancelReason": "전액환불"}
                                  ]
                                }
                                """)));

        TuitionPaymentGatewayPort.RefundResult result =
                client.refund("pk_refund_auto", null, "전액환불", null);

        assertThat(result.success()).isTrue();

        // A generated Idempotency-Key should still be sent
        wm.verify(postRequestedFor(urlMatching("/v1/payments/.+/cancel"))
                .withHeader("Idempotency-Key", matching("refund-.+")));
    }

    // ─── verifyWebhook ────────────────────────────────────────────────────────

    @Test
    void verifyWebhook_validSignature_returnsValid() throws Exception {
        String rawBody = """
                {"eventType":"PAYMENT_STATUS_CHANGED","data":{"orderId":"order-wh-tuition-001"}}
                """.trim();
        long timestamp = Instant.now().getEpochSecond();
        String signatureHeader = buildSignatureHeader(timestamp, rawBody, WEBHOOK_SECRET);

        TuitionPaymentGatewayPort.WebhookVerificationResult result =
                client.verifyWebhook(signatureHeader, rawBody);

        assertThat(result.valid()).isTrue();
        assertThat(result.eventType()).isEqualTo("PAYMENT_STATUS_CHANGED");
    }

    @Test
    void verifyWebhook_wrongSecret_returnsInvalid() throws Exception {
        String rawBody = "{\"eventType\":\"PAYMENT_STATUS_CHANGED\"}";
        long timestamp = Instant.now().getEpochSecond();
        String signatureHeader = buildSignatureHeader(timestamp, rawBody, "wrong_secret");

        TuitionPaymentGatewayPort.WebhookVerificationResult result =
                client.verifyWebhook(signatureHeader, rawBody);

        assertThat(result.valid()).isFalse();
    }

    @Test
    void verifyWebhook_timestampTooOld_returnsInvalid() throws Exception {
        String rawBody = "{\"eventType\":\"PAYMENT_STATUS_CHANGED\"}";
        long oldTimestamp = Instant.now().getEpochSecond() - 360;
        String signatureHeader = buildSignatureHeader(oldTimestamp, rawBody, WEBHOOK_SECRET);

        TuitionPaymentGatewayPort.WebhookVerificationResult result =
                client.verifyWebhook(signatureHeader, rawBody);

        assertThat(result.valid()).isFalse();
    }

    @Test
    void verifyWebhook_nullSignature_returnsInvalid() {
        TuitionPaymentGatewayPort.WebhookVerificationResult result =
                client.verifyWebhook(null, "{}");

        assertThat(result.valid()).isFalse();
    }

    @Test
    void refund_4xxError_throwsException() {
        wm.stubFor(post(urlMatching("/v1/payments/.+/cancel"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "ALREADY_CANCELED_PAYMENT",
                                  "message": "이미 취소된 결제입니다."
                                }
                                """)));

        TuitionPaymentGatewayPort.RefundResult result =
                client.refund("pk_already_canceled", BigDecimal.valueOf(50000),
                        "중복 취소", "idem-4xx-001");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("ALREADY_CANCELED_PAYMENT");
    }

    @Test
    void requestPayment_nullOrderId_returnsFailure() {
        TuitionPaymentGatewayPort.TuitionPaymentRequest req = new TuitionPaymentGatewayPort.TuitionPaymentRequest(
                null, "수강료", BigDecimal.valueOf(100000),
                "홍길동", "hong@example.com", null, null
        );

        TuitionPaymentGatewayPort.PaymentRequestResult result = client.requestPayment(req);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("INVALID_ORDER_ID");
    }

    @Test
    void registerSeller_stub_returnsFalse() {
        com.lumie.tuition.domain.entity.MerchantProfile profile =
                com.lumie.tuition.domain.entity.MerchantProfile.create(
                        "000-00-00000", "홍길동", "004", "123-456789", "홍길동"
                );

        TuitionPaymentGatewayPort.SellerRegistrationResult result =
                client.registerSeller(profile, null);

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("NOT_IMPLEMENTED");
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
