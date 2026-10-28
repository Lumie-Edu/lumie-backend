package com.lumie.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

/**
 * Filter that logs each HTTP request with method, path, status, and latency.
 *
 * <p>Runs after {@link RequestContextFilter}; the downstream security chain has
 * completed when this filter writes the access event. Request-specific access
 * fields are emitted as structured arguments to avoid duplicate JSON keys with MDC.
 */
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("access-log");

    private final List<String> excludePaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AccessLogFilter(List<String> excludePaths) {
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
        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            refreshIdentityMdc(request);
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            String method = request.getMethod();
            String path = request.getRequestURI();
            String pathTemplate = resolvePathTemplate(request, path);
            int status = response.getStatus();
            String outcome = status >= 500 ? "server_error"
                    : status >= 400 ? "client_error"
                    : "success";

            log.info("HTTP request {} {} {} {}ms",
                    method, pathTemplate, status, latencyMs,
                    keyValue("eventType", "http_request"),
                    keyValue("httpMethod", method),
                    keyValue("pathTemplate", pathTemplate),
                    keyValue("rawPath", path),
                    keyValue("httpStatus", status),
                    keyValue("latencyMs", latencyMs),
                    keyValue("outcome", outcome),
                    keyValue("clientIp", clientIp(request)),
                    keyValue("userAgent", request.getHeader("User-Agent")));
        }
    }

    /**
     * Servlet filters run before Spring Security, so RequestContextFilter's
     * initial MDC snapshot may predate session authentication. Read the
     * request-scoped holders after the downstream chain has completed.
     */
    private void refreshIdentityMdc(HttpServletRequest request) {
        MDC.remove("tenantId");
        MDC.remove("tenantSlug");
        MDC.remove("userId");
        MDC.remove("userRole");

        if (request.getRequestURI().startsWith("/internal/")) {
            return;
        }

        Long tenantId = TenantContextHolder.getTenantId();
        String tenantSlug = TenantContextHolder.getTenant();
        if (tenantId != null) MDC.put("tenantId", tenantId.toString());
        if (tenantSlug != null && !tenantSlug.isBlank()) MDC.put("tenantSlug", tenantSlug);

        if (!Boolean.TRUE.equals(request.getAttribute(RequestContextFilter.AUTHENTICATED_SESSION_ATTRIBUTE))) {
            return;
        }
        Long userId = UserContextHolder.getUserId();
        if (userId != null) MDC.put("userId", userId.toString());
        if (UserContextHolder.getUserRole() != null) {
            MDC.put("userRole", UserContextHolder.getUserRole().name());
        }
    }

    private String resolvePathTemplate(HttpServletRequest request, String fallback) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern instanceof String value && !value.isBlank() ? value : fallback;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
