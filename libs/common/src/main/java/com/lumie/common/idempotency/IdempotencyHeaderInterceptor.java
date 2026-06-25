package com.lumie.common.idempotency;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Stripe-style {@code Idempotency-Key} gate for billing-surface mutations.
 *
 * <p>Enforces the header at the HTTP boundary so individual controllers
 * cannot silently skip idempotency. Controllers still call
 * {@link IdempotencyService#executeOnce} explicitly — this interceptor only
 * guarantees the header is present and well-formed before the handler runs.
 *
 * <p>Scope (intentionally narrow):
 * <ul>
 *   <li>HTTP method ∈ {POST, PUT, PATCH, DELETE}</li>
 *   <li>Request path matches one of {@link #ENFORCED_PATTERNS} — only the
 *       billing surface (subscribe, change plan, cancel, billing-keys,
 *       payments, alimtalk credits). Replay-safety matters here because
 *       these calls move money or provision paid resources.</li>
 *   <li>Path NOT matched by any {@link #EXEMPT_PATTERNS} entry — defense
 *       in depth for webhook callbacks that might be misrouted under a
 *       billing path (PG-managed retry, not client-driven idempotency).</li>
 * </ul>
 *
 * <p>Everything outside the billing surface — auth (login/refresh/logout),
 * attendance, lecture, class, student, exam, etc. — is intentionally not
 * gated. Auth endpoints must mint fresh state on every call (replay
 * semantics are wrong for them). The other domains do not currently issue
 * money-moving mutations; if a future endpoint needs replay-safety, add
 * its path prefix to {@link #ENFORCED_PATTERNS} explicitly and pair it
 * with an ArchUnit guard.
 *
 * <p>Header value validation: 16–128 chars, ASCII subset
 * {@code [A-Za-z0-9._:\-]} (covers UUID v4, Stripe-style {@code req_xxx},
 * and base64url tokens). Rejecting wider character sets keeps log-injection
 * and header-smuggling vectors closed.
 */
public class IdempotencyHeaderInterceptor implements HandlerInterceptor {

    public static final String HEADER_NAME = "Idempotency-Key";
    public static final int MIN_LENGTH = 16;
    public static final int MAX_LENGTH = 128;

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final List<String> ENFORCED_PATTERNS = List.of(
            "/v1/billing/**",
            "/v1/billing-keys/**",
            "/v1/subscriptions/**",
            "/v1/payments/**",
            "/v1/alimtalk/credits/**"
    );
    private static final List<String> EXEMPT_PATTERNS = List.of(
            "/internal/webhooks/**",
            "/v1/internal/webhooks/**"
    );
    public static final Pattern KEY_FORMAT = Pattern.compile("^[A-Za-z0-9._:\\-]+$");

    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if (!isEnforced(request)) {
            return true;
        }

        String value = request.getHeader(HEADER_NAME);
        if (value == null || value.isBlank()) {
            throw new BusinessException(
                    CommonErrorCode.MISSING_IDEMPOTENCY_KEY,
                    "Idempotency-Key header is required for " + request.getMethod()
                            + " " + request.getRequestURI());
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH
                || !KEY_FORMAT.matcher(value).matches()) {
            throw new BusinessException(
                    CommonErrorCode.INVALID_IDEMPOTENCY_KEY_FORMAT,
                    "Idempotency-Key must be " + MIN_LENGTH + "–" + MAX_LENGTH
                            + " chars matching [A-Za-z0-9._:-]");
        }
        return true;
    }

    private boolean isEnforced(HttpServletRequest request) {
        if (!MUTATING_METHODS.contains(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        for (String exempt : EXEMPT_PATTERNS) {
            if (matcher.match(exempt, path)) {
                return false;
            }
        }
        for (String pattern : ENFORCED_PATTERNS) {
            if (matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
