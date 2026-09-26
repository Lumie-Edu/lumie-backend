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
 * Filter that populates user context from SecurityContext and manages
 * request correlation. Tenant context is bound from the request host before
 * this filter by the tenant module's HostTenantFilter.
 */
public class RequestContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String HOST_TENANT_SLUG_ATTRIBUTE = "lumie.hostTenantSlug";
    public static final String HOST_TENANT_ID_ATTRIBUTE = "lumie.hostTenantId";
    public static final String HOST_TENANT_RESOLUTION_ATTRIBUTE = "lumie.hostTenantResolution";

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

            if (!populateFromSecurityContext() && request.getRequestURI().startsWith("/internal/")) {
                populateFromInternalHeaders(request);
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

        // The authentication filter already set TenantContextHolder and UserContextHolder.
        if (TenantContextHolder.getTenant() != null) {
            log.debug("Context populated from authenticated session: tenant={}, userId={}",
                    TenantContextHolder.getTenant(), UserContextHolder.getUserId());
            return true;
        }

        return false;
    }

    /**
     * Worker and monolith-internal calls remain HMAC-authenticated and carry
     * tenant context explicitly. Public browser requests never use these
     * headers to select a tenant.
     */
    private void populateFromInternalHeaders(HttpServletRequest request) {
        String tenantSlug = request.getHeader("X-Tenant-Slug");
        if (tenantSlug == null || tenantSlug.isBlank()) {
            return;
        }
        TenantContextHolder.setTenant(tenantSlug);

        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId != null && !tenantId.isBlank()) {
            try {
                TenantContextHolder.setTenantId(Long.parseLong(tenantId));
            } catch (NumberFormatException exception) {
                log.warn("Invalid X-Tenant-Id header value: {}", tenantId);
            }
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
