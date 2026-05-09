package com.lumie.billing.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "paddle")
public class PaddleConfig {

    private boolean enabled = false;
    private boolean sandbox = true;
    private String apiKey;
    private String webhookSecret;
    private String clientToken;

    public String getApiBaseUrl() {
        return sandbox
                ? "https://sandbox-api.paddle.com"
                : "https://api.paddle.com";
    }
}
