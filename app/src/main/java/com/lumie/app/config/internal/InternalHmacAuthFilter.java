package com.lumie.app.config.internal;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.tenant.api.TenantService;
import com.lumie.tenant.api.TenantService.TenantData;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Authenticates {@code /internal/**} endpoints via shared-secret HMAC-SHA256.
 *
 * <p>Required headers on every request:
 * <ul>
 *   <li>{@code X-Tenant-Slug} — non-blank tenant slug. <b>Bound into the signature</b>:
 *       the filter recomputes the HMAC using this header value, so a request whose
 *       header doesn't match the tenant the caller signed for fails verification.
 *       This blocks a worker authorized for tenant A from acting on tenant B.</li>
 *   <li>{@code X-Timestamp}  — Unix seconds; rejected if skewed more than 300s.</li>
 *   <li>{@code X-Signature}  — hex-encoded
 *       HMAC-SHA256({@code timestamp + "\n" + tenantSlug + "\n" + body}).</li>
 * </ul>
 *
 * <p>On verification success, the {@link SecurityContextHolder} is populated
 * with a synthetic {@code ROLE_INTERNAL} authentication so the
 * {@code SecurityFilterChain.authenticated()} rule passes.
 *
 * <p>The body is buffered through {@link CachedBodyHttpServletRequest} so the
 * downstream controller can still read the {@code @RequestBody}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternalHmacAuthFilter extends OncePerRequestFilter {

    public static final String SIGNATURE_HEADER = "X-Signature";
    public static final String TIMESTAMP_HEADER = "X-Timestamp";
    public static final String TENANT_SLUG_HEADER = "X-Tenant-Slug";

    private static final String INTERNAL_PATH_PREFIX = "/internal/";
    private static final long MAX_TIMESTAMP_SKEW_SECONDS = 300L;
    private static final long MAX_BODY_BYTES = 1L * 1024 * 1024; // 1 MiB
    private static final int MIN_SECRET_BYTES = 32;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String INTERNAL_PRINCIPAL = "internal-service";

    private final InternalAuthProperties properties;
    private final TenantService tenantService;
    private SecretKeySpec keySpec;

    @PostConstruct
    void init() {
        byte[] secretBytes = properties.hmacSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "lumie.internal.hmac-secret must be at least "
                            + MIN_SECRET_BYTES + " bytes; got " + secretBytes.length);
        }
        this.keySpec = new SecretKeySpec(secretBytes, HMAC_ALGORITHM);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String tenantSlug = request.getHeader(TENANT_SLUG_HEADER);
        if (isBlank(tenantSlug)) {
            reject(response, "missing X-Tenant-Slug header");
            return;
        }

        String timestampHeader = request.getHeader(TIMESTAMP_HEADER);
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader);
        } catch (NumberFormatException | NullPointerException e) {
            reject(response, "missing or invalid X-Timestamp header");
            return;
        }

        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestamp) > MAX_TIMESTAMP_SKEW_SECONDS) {
            reject(response, "timestamp skew too large");
            return;
        }

        String providedSignature = request.getHeader(SIGNATURE_HEADER);
        if (isBlank(providedSignature)) {
            reject(response, "missing X-Signature header");
            return;
        }

        long contentLength = request.getContentLengthLong();
        if (contentLength > MAX_BODY_BYTES) {
            reject(response, "body too large");
            return;
        }
        byte[] body = readBoundedBody(request, MAX_BODY_BYTES);
        if (body == null) {
            reject(response, "body too large");
            return;
        }
        CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request, body);

        String expectedSignature = computeSignature(timestamp, tenantSlug, body);
        if (!constantTimeEquals(expectedSignature, providedSignature.toLowerCase(Locale.ROOT))) {
            reject(response, "signature mismatch");
            return;
        }

        // Slug is verified via HMAC payload binding at this point. Resolve to tenantId
        // so RLS sees a populated context — setTenant alone leaves rows invisible.
        TenantData tenant = tenantService.getTenantBySlug(tenantSlug).orElse(null);
        if (tenant == null || !tenant.isActive()) {
            reject(response, "unknown or inactive tenant");
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(
                new PreAuthenticatedAuthenticationToken(
                        INTERNAL_PRINCIPAL,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_INTERNAL")))
        );
        TenantContextHolder.setTenant(tenant.slug());
        TenantContextHolder.setTenantId(tenant.id());

        try {
            chain.doFilter(wrapped, response);
        } finally {
            SecurityContextHolder.clearContext();
            TenantContextHolder.clear();
        }
    }

    private String computeSignature(long timestamp, String tenantSlug, byte[] body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(keySpec);
            mac.update(Long.toString(timestamp).getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '\n');
            mac.update(tenantSlug.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '\n');
            mac.update(body);
            return toHex(mac.doFinal());
        } catch (Exception e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }

    private static byte[] readBoundedBody(HttpServletRequest request, long limit) throws IOException {
        try (var in = request.getInputStream();
             var out = new java.io.ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            long total = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > limit) {
                    return null;
                }
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static boolean constantTimeEquals(String expected, String provided) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                provided.getBytes(StandardCharsets.US_ASCII));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void reject(HttpServletResponse response, String reason) throws IOException {
        log.warn("Rejected /internal/** request: {}", reason);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"unauthorized\",\"reason\":\"" + reason + "\"}");
    }
}
