package com.lumie.app.config.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Per-route rate-limit configuration. Each rule pairs an Ant-style path
 * pattern (matched by Spring's {@code AntPathMatcher}) with a token-bucket
 * capacity and refill window. Rules are evaluated in declaration order;
 * first match wins.
 *
 * @param rules ordered list of (path pattern, capacity, window) triples
 */
@ConfigurationProperties(prefix = "lumie.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        List<Rule> rules
) {
    public RateLimitProperties {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    public record Rule(
            String pathPattern,
            int capacity,
            Duration window
    ) {}
}
