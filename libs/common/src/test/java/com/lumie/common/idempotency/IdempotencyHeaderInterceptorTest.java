package com.lumie.common.idempotency;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for {@link IdempotencyHeaderInterceptor} — no Spring context,
 * no DB. Verifies the four enforcement axes (method, path, header presence,
 * format) and the webhook exemption.
 */
@DisplayName("IdempotencyHeaderInterceptor")
class IdempotencyHeaderInterceptorTest {

    private IdempotencyHeaderInterceptor interceptor;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new IdempotencyHeaderInterceptor();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("non-mutating requests bypass enforcement")
    class NonMutating {

        @Test
        void getRequestPasses() {
            assertThat(interceptor.preHandle(req("GET", "/v1/billing-keys", null), response, new Object()))
                    .isTrue();
        }

        @Test
        void headRequestPasses() {
            assertThat(interceptor.preHandle(req("HEAD", "/v1/billing-keys", null), response, new Object()))
                    .isTrue();
        }

        @Test
        void optionsRequestPasses() {
            assertThat(interceptor.preHandle(req("OPTIONS", "/v1/billing-keys", null), response, new Object()))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("non-enforced paths bypass — actuator, root, v2, non-billing /v1 domains")
    class NonEnforcedPath {

        @Test
        void actuatorPostPasses() {
            assertThat(interceptor.preHandle(req("POST", "/actuator/refresh", null), response, new Object()))
                    .isTrue();
        }

        @Test
        void rootPostPasses() {
            assertThat(interceptor.preHandle(req("POST", "/", null), response, new Object()))
                    .isTrue();
        }

        @Test
        void v2PostPasses() {
            assertThat(interceptor.preHandle(req("POST", "/v2/anything", null), response, new Object()))
                    .isTrue();
        }

        /* Most non-billing /v1 domains — replay-safety not required by these flows.
         * Auth (login/refresh/logout): each call must mint fresh state.
         * Domain APIs (lectures, regular student CRUD, etc): no money-moving mutations today.
         * If any future endpoint needs Idempotency-Key, add its prefix to
         * ENFORCED_PATTERNS explicitly. */

        @Test
        void v1LoginPostPasses() {
            assertThat(interceptor.preHandle(req("POST", "/v1/login", null), response, new Object()))
                    .isTrue();
        }

        @Test
        void v1RefreshPostPasses() {
            assertThat(interceptor.preHandle(req("POST", "/v1/refresh", null), response, new Object()))
                    .isTrue();
        }

        @Test
        void v1LogoutPostPasses() {
            assertThat(interceptor.preHandle(req("POST", "/v1/logout", null), response, new Object()))
                    .isTrue();
        }

        @Test
        void v1StudentsPostPasses() {
            assertThat(interceptor.preHandle(req("POST", "/v1/students", null), response, new Object()))
                    .isTrue();
        }

        @Test
        void v1LecturesPatchPasses() {
            assertThat(interceptor.preHandle(req("PATCH", "/v1/lectures/42", null), response, new Object()))
                    .isTrue();
        }

    }

    @Nested
    @DisplayName("webhook exemption")
    class Webhook {

        @Test
        void internalWebhookPostPasses() {
            assertThat(interceptor.preHandle(
                    req("POST", "/internal/webhooks/toss/billing", null), response, new Object()))
                    .isTrue();
        }

        @Test
        void v1InternalWebhookPostPasses() {
            assertThat(interceptor.preHandle(
                    req("POST", "/v1/internal/webhooks/toss/billing", null), response, new Object()))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("missing or blank header rejects with MISSING_IDEMPOTENCY_KEY")
    class MissingHeader {

        @Test
        void postWithoutHeaderThrows() {
            assertThatThrownBy(() -> interceptor.preHandle(
                    req("POST", "/v1/billing/subscribe", null), response, new Object()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.MISSING_IDEMPOTENCY_KEY);
        }

        @Test
        void putWithBlankHeaderThrows() {
            assertThatThrownBy(() -> interceptor.preHandle(
                    req("PUT", "/v1/alimtalk/credits/auto-recharge", "   "), response, new Object()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.MISSING_IDEMPOTENCY_KEY);
        }

        @Test
        void deleteWithoutHeaderThrows() {
            assertThatThrownBy(() -> interceptor.preHandle(
                    req("DELETE", "/v1/subscriptions/foo", null), response, new Object()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.MISSING_IDEMPOTENCY_KEY);
        }

        @Test
        void patchWithoutHeaderThrows() {
            assertThatThrownBy(() -> interceptor.preHandle(
                    req("PATCH", "/v1/subscriptions/foo", null), response, new Object()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.MISSING_IDEMPOTENCY_KEY);
        }

        @Test
        void studentBulkImportWithoutHeaderThrows() {
            assertThatThrownBy(() -> interceptor.preHandle(
                    req("POST", "/v1/students/bulk", null), response, new Object()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.MISSING_IDEMPOTENCY_KEY);
        }
    }

    @Nested
    @DisplayName("malformed header rejects with INVALID_IDEMPOTENCY_KEY_FORMAT")
    class InvalidFormat {

        @Test
        void tooShortThrows() {
            String key = "abc-123";  // 7 chars < 16
            assertThatThrownBy(() -> interceptor.preHandle(
                    req("POST", "/v1/billing/subscribe", key), response, new Object()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_IDEMPOTENCY_KEY_FORMAT);
        }

        @Test
        void tooLongThrows() {
            String key = "a".repeat(129);  // > 128
            assertThatThrownBy(() -> interceptor.preHandle(
                    req("POST", "/v1/billing/subscribe", key), response, new Object()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_IDEMPOTENCY_KEY_FORMAT);
        }

        @Test
        void disallowedCharsThrow() {
            String key = "key with spaces and 한글";
            assertThatThrownBy(() -> interceptor.preHandle(
                    req("POST", "/v1/billing/subscribe", key), response, new Object()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_IDEMPOTENCY_KEY_FORMAT);
        }

        @Test
        void controlCharsThrow() {
            String key = "abcdefghijklmnop\n123";  // newline injection
            assertThatThrownBy(() -> interceptor.preHandle(
                    req("POST", "/v1/billing/subscribe", key), response, new Object()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.INVALID_IDEMPOTENCY_KEY_FORMAT);
        }
    }

    @Nested
    @DisplayName("valid keys pass")
    class ValidFormat {

        @Test
        void uuidV4Passes() {
            String key = "550e8400-e29b-41d4-a716-446655440000";  // 36 chars
            assertThat(interceptor.preHandle(
                    req("POST", "/v1/billing/subscribe", key), response, new Object()))
                    .isTrue();
        }

        @Test
        void stripeStylePrefixedKeyPasses() {
            String key = "req_abc123XYZ-456_def";  // 21 chars
            assertThat(interceptor.preHandle(
                    req("POST", "/v1/billing/subscribe", key), response, new Object()))
                    .isTrue();
        }

        @Test
        void minLengthBoundaryPasses() {
            String key = "a".repeat(IdempotencyHeaderInterceptor.MIN_LENGTH);
            assertThat(interceptor.preHandle(
                    req("POST", "/v1/billing/subscribe", key), response, new Object()))
                    .isTrue();
        }

        @Test
        void maxLengthBoundaryPasses() {
            String key = "a".repeat(IdempotencyHeaderInterceptor.MAX_LENGTH);
            assertThat(interceptor.preHandle(
                    req("POST", "/v1/billing/subscribe", key), response, new Object()))
                    .isTrue();
        }

        @Test
        void dotsColonsAndUnderscoresPass() {
            String key = "tenant_15:subscribe:abc.123-xyz";  // 31 chars
            assertThat(interceptor.preHandle(
                    req("POST", "/v1/billing/subscribe", key), response, new Object()))
                    .isTrue();
        }

        @Test
        void studentBulkImportWithValidHeaderPasses() {
            String key = "550e8400-e29b-41d4-a716-446655440000";
            assertThat(interceptor.preHandle(
                    req("POST", "/v1/students/bulk", key), response, new Object()))
                    .isTrue();
        }
    }

    private static HttpServletRequest req(String method, String uri, String idempotencyKey) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.setRequestURI(uri);
        if (idempotencyKey != null) {
            req.addHeader(IdempotencyHeaderInterceptor.HEADER_NAME, idempotencyKey);
        }
        return req;
    }
}
