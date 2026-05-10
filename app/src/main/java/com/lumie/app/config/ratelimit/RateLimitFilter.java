package com.lumie.app.config.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Per-IP token-bucket rate limiter for sensitive endpoints (login,
 * registration, bulk import, OMR batch confirm).
 *
 * <p>Buckets are keyed on {@code (rule.pathPattern, clientIp)} and live
 * in-process — accurate within a single pod but a 5/min limit becomes
 * effectively {@code 5 × pod-count}/min cluster-wide. Acceptable starting
 * point: per-pod IP-bucket already bounds a single attacker.
 *
 * <p>Runs at the highest precedence so an exhausted bucket short-circuits
 * before JWT/HMAC/Tenant filters do their work.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final RateLimitProperties properties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!properties.enabled() || properties.rules().isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        RateLimitProperties.Rule rule = matchRule(request);
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }

        Bucket bucket = bucketFor(rule, clientIp(request));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Limit", String.valueOf(rule.capacity()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
        log.warn("Rate limit exceeded: ip={}, path={}, rule={}, retryAfter={}s",
                clientIp(request), request.getRequestURI(), rule.pathPattern(), retryAfterSeconds);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.capacity()));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"errorCode\":\"RATE_LIMITED\",\"message\":\"Too many requests\"," +
                "\"retryAfterSeconds\":" + retryAfterSeconds + "}");
    }

    private RateLimitProperties.Rule matchRule(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI();
        for (RateLimitProperties.Rule rule : properties.rules()) {
            if (PATH_MATCHER.match(rule.pathPattern(), path)) {
                return rule;
            }
        }
        return null;
    }

    private Bucket bucketFor(RateLimitProperties.Rule rule, String ip) {
        String key = rule.pathPattern() + "|" + ip;
        return buckets.computeIfAbsent(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(rule.capacity())
                        .refillIntervally(rule.capacity(), rule.window())
                        .build())
                .build());
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
