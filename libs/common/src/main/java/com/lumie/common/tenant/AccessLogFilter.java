package com.lumie.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Filter that logs each HTTP request with method, path, status, and latency.
 *
 * <p>Runs after {@link RequestContextFilter} so MDC already contains
 * tenant and user context. Adds request-specific MDC keys temporarily
 * for the access log line, then removes them.
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
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            String method = request.getMethod();
            String path = request.getRequestURI();
            int status = response.getStatus();

            MDC.put("httpMethod", method);
            MDC.put("httpPath", path);
            MDC.put("httpStatus", String.valueOf(status));
            MDC.put("latencyMs", String.valueOf(latencyMs));

            log.info("{} {} {} {}ms", method, path, status, latencyMs);

            MDC.remove("httpMethod");
            MDC.remove("httpPath");
            MDC.remove("httpStatus");
            MDC.remove("latencyMs");
        }
    }
}
