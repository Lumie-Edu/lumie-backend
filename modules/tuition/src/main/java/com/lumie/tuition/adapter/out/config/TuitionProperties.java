package com.lumie.tuition.adapter.out.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Immutable configuration properties for the tuition module.
 * Bound via constructor (record-style) — no @Setter needed.
 */
@ConfigurationProperties(prefix = "tuition")
public record TuitionProperties(Toss toss) {

    public TuitionProperties {
        if (toss == null) {
            toss = new Toss(true, null, null, null);
        }
    }

    public record Toss(boolean sandbox, String secretKey, String clientKey, String webhookSecret) {
    }
}
