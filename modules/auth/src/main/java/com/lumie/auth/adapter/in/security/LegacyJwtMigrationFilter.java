package com.lumie.auth.adapter.in.security;

import com.lumie.auth.adapter.in.web.CookieUtils;
import com.lumie.common.exception.ProblemResponseWriter;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.auth.adapter.in.web.SessionMetadataExtractor;
import com.lumie.auth.adapter.out.config.SessionConfig;
import com.lumie.auth.application.port.out.UserLookupPort;
import com.lumie.auth.application.service.LoginSessionHelper;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Temporary, verification-only bridge for valid legacy refresh JWT cookies. */
@Component
@RequiredArgsConstructor
public class LegacyJwtMigrationFilter extends OncePerRequestFilter {
    private static final String PATH = "/v1/auth/migrate-legacy-session";
    private static final String REFRESH_COOKIE = "lumie_refresh_token";
    private static final String ACCESS_COOKIE = "lumie_access_token";
    private final StringRedisTemplate redis;
    private final UserLookupPort users;
    private final LoginSessionHelper sessions;
    private final CookieUtils cookieUtils;
    private final SessionConfig sessionConfig;
    private final SameOriginPolicy sameOriginPolicy;
    private final ProblemResponseWriter problems;

    @Value("${legacy-jwt.secret-key:}") private String secret;
    @Value("${legacy-jwt.sunset-at:2026-12-31T14:59:59Z}") private Instant sunsetAt;

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !PATH.equals(request.getRequestURI());
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                               FilterChain chain) throws IOException, ServletException {
        if (!"POST".equals(request.getMethod()) || secret.isBlank() || !Instant.now().isBefore(sunsetAt)) {
            problems.write(request, response, HttpServletResponse.SC_NOT_FOUND, CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "Not found"); return;
        }
        if (!sameOriginPolicy.isAllowed(request)) { problems.write(request, response, HttpServletResponse.SC_FORBIDDEN, CommonErrorCode.FORBIDDEN.getCode(), "Cross-origin request rejected"); return; }
        String token = cookie(request, REFRESH_COOKIE);
        if (token == null) { response.setStatus(HttpServletResponse.SC_NO_CONTENT); return; }
        try {
            Claims claims = Jwts.parser().verifyWith(key()).requireIssuer("lumie-auth-svc").build()
                    .parseSignedClaims(token).getPayload();
            if (!"refresh".equals(claims.get("type", String.class))) { response.setStatus(204); return; }
            String slug = claims.get("tenant_slug", String.class);
            Long tenantId = claims.get("tenant_id", Long.class);
            String sid = claims.get("sid", String.class);
            Long userId = Long.valueOf(claims.getSubject());
            if (!slug.equals(TenantContextHolder.getTenant()) || !tenantId.equals(TenantContextHolder.getTenantId())
                    || Boolean.TRUE.equals(redis.hasKey("auth:blacklist:" + claims.getId()))
                    || Boolean.TRUE.equals(redis.hasKey("auth:session-revoked:" + slug + ":" + sid))
                    || !Boolean.TRUE.equals(redis.hasKey("auth:refresh:" + userId + ":" + claims.getId()))) {
                response.setStatus(204); return;
            }
            var user = TenantContextHolder.withinContext(slug, tenantId,
                    () -> users.findById(userId).orElse(null));
            if (user == null || !user.enabled() || !tenantId.equals(user.tenantId())) { response.setStatus(204); return; }
            // Redis DELETE is atomic: only one concurrent request can consume
            // the legacy refresh credential and mint an opaque session.
            if (!Boolean.TRUE.equals(redis.delete("auth:refresh:" + userId + ":" + claims.getId()))) {
                response.setStatus(204); return;
            }
            AuthSession session = sessions.create(user, slug, tenantId, SessionMetadataExtractor.extract(request));
            response.addHeader(HttpHeaders.SET_COOKIE, cookieUtils.createSessionCookie(session.id()).toString());
            response.addHeader(HttpHeaders.SET_COOKIE, expire(REFRESH_COOKIE));
            response.addHeader(HttpHeaders.SET_COOKIE, expire(ACCESS_COOKIE));
            response.setHeader("X-Lumie-Migrated-Path", user.role() == Role.STUDENT ? "/dashboard" : "/admin");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException exception) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }
    private SecretKey key() { return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); }
    private String cookie(HttpServletRequest request, String name) { if (request.getCookies() == null) return null; for (Cookie c : request.getCookies()) if (name.equals(c.getName())) return c.getValue(); return null; }
    private String expire(String name) { return ResponseCookie.from(name, "").httpOnly(true).secure(sessionConfig.isCookieSecure()).sameSite(sessionConfig.getCookieSameSite()).path("/").maxAge(0).build().toString(); }
}
