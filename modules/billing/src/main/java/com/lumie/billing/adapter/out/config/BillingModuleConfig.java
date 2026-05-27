package com.lumie.billing.adapter.out.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BillingProperties.class)
@RequiredArgsConstructor
public class BillingModuleConfig {

    private final BillingProperties billingProperties;

    /**
     * RestClient for Toss Payments API calls from the billing module.
     * Base URL switches between sandbox and live based on {@code billing.toss.sandbox}.
     * Authorization header is NOT set here — TossPaymentClient adds it per-request
     * because the secretKey may be absent during tests (no auth header needed for WireMock).
     */
    @Bean("tossBillingRestClient")
    public RestClient tossBillingRestClient() {
        String baseUrl = billingProperties.toss().sandbox()
                ? "https://sandbox-api.tosspayments.com"
                : "https://api.tosspayments.com";
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
