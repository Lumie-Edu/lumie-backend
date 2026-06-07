package com.lumie.common.tenant;

import com.lumie.common.auth.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Filter that populates tenant and user context from SecurityContext (JWT)
 * or X-headers (legacy/fallback).
 */
public class RequestContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String TENANT_SLUG_HEADER = "X-Tenant-Slug";
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final String TRACEPARENT_HEADER = "traceparent";
    private static final int MAX_REQUEST_ID_LENGTH = 128;
    private static final int TRACE_ID_LENGTH = 32;

    private final List<String> excludePaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RequestContextFilter(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return excludePaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            setRequestCorrelation(request, response);

            if (!populateFromSecurityContext()) {
                populateFromHeaders(request);
            }

            setMdc();

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
            TenantContextHolder.clear();
            UserContextHolder.clear();
        }
    }

    private void setRequestCorrelation(HttpServletRequest request, HttpServletResponse response) {
        String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
        MDC.put("requestId", requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        String traceId = extractTraceId(request.getHeader(TRACEPARENT_HEADER));
        if (traceId != null) {
            MDC.put("traceId", traceId);
        }
    }

    private String resolveRequestId(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String trimmed = headerValue.trim();
        if (trimmed.length() > MAX_REQUEST_ID_LENGTH) {
            return UUID.randomUUID().toString();
        }
        return trimmed;
    }

    private String extractTraceId(String traceparent) {
        if (traceparent == null || traceparent.isBlank()) {
            return null;
        }
        String[] parts = traceparent.split("-", -1);
        if (parts.length < 4) {
            return null;
        }
        String traceId = parts[1].toLowerCase(Locale.ROOT);
        if (traceId.length() != TRACE_ID_LENGTH || isAllZeros(traceId) || !isHex(traceId)) {
            return null;
        }
        return traceId;
    }

    private boolean isAllZeros(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }

    private boolean isHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) == -1) {
                return false;
            }
        }
        return true;
    }

    private boolean populateFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return false;
        }

        // JwtAuthenticationFilter already set TenantContextHolder and UserContextHolder
        if (TenantContextHolder.getTenant() != null) {
            log.debug("Context populated from JWT: tenant={}, userId={}",
                    TenantContextHolder.getTenant(), UserContextHolder.getUserId());
            return true;
        }

        return false;
    }

    private void populateFromHeaders(HttpServletRequest request) {
        String tenantSlug = request.getHeader(TENANT_SLUG_HEADER);
        if (tenantSlug == null || tenantSlug.isBlank()) {
            log.debug("No tenant context for request: {}", request.getRequestURI());
            return;
        }

        TenantContextHolder.setTenant(tenantSlug);

        String tenantId = request.getHeader(TENANT_ID_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            try {
                TenantContextHolder.setTenantId(Long.parseLong(tenantId));
            } catch (NumberFormatException e) {
                log.warn("Invalid {} header value: {}", TENANT_ID_HEADER, tenantId);
            }
        }

        String userId = request.getHeader(USER_ID_HEADER);
        if (userId != null && !userId.isBlank()) {
            try {
                UserContextHolder.setUserId(Long.parseLong(userId));
            } catch (NumberFormatException e) {
                log.warn("Invalid {} header value: {}", USER_ID_HEADER, userId);
            }
        }

        String userRole = request.getHeader(USER_ROLE_HEADER);
        if (userRole != null && !userRole.isBlank()) {
            UserContextHolder.setUserRole(userRole);
        }
    }

    private void setMdc() {
        String tenant = TenantContextHolder.getTenant();
        if (tenant != null) MDC.put("tenantSlug", tenant);
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) MDC.put("tenantId", tenantId.toString());
        Long userId = UserContextHolder.getUserId();
        if (userId != null) MDC.put("userId", userId.toString());
        Role role = UserContextHolder.getUserRole();
        if (role != null) MDC.put("userRole", role.name());
    }
}
