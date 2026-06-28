package com.lumie.notification.adapter.out.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lumie.sms")
public record SmsProperties(
        String provider,
        String fromNumber,
        int dispatchBatchSize,
        int maxDispatchAttempts,
        Solapi solapi
) {
    public SmsProperties {
        provider = provider == null || provider.isBlank() ? "mock" : provider;
        dispatchBatchSize = dispatchBatchSize <= 0 ? 50 : dispatchBatchSize;
        maxDispatchAttempts = maxDispatchAttempts <= 0 ? 3 : maxDispatchAttempts;
        solapi = solapi == null ? new Solapi(null, null, "https://api.solapi.com", null) : solapi;
    }

    public record Solapi(
            String apiKey,
            String apiSecret,
            String baseUrl,
            String webhookToken
    ) {
        public Solapi {
            baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://api.solapi.com" : baseUrl;
        }
    }
}
