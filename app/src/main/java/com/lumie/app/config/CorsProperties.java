package com.lumie.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "lumie.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
