package com.lumie.app.config.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lumie.internal")
public record InternalAuthProperties(String hmacSecret) {
}
