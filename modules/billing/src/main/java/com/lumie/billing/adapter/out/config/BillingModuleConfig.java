package com.lumie.billing.adapter.out.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BillingProperties.class)
public class BillingModuleConfig {

    /**
     * RestClient for Toss Payments API calls from the billing module.
     *
     * <p>Toss exposes a single host for both sandbox and live —
     * {@code https://api.tosspayments.com}. The environment is selected by the
     * secret key prefix that {@link TossPaymentClient} sends in the Basic Auth
     * header: {@code test_sk_*} routes to the sandbox ledger, {@code live_sk_*}
     * to production. There is no separate {@code sandbox-api.tosspayments.com}
     * hostname (a previous version of this config invented one and got
     * Cloudflare-403'd because the domain does not resolve).
     *
     * <p>Authorization header is NOT set here — TossPaymentClient adds it
     * per-request because the secretKey may be absent during WireMock tests.
     */
    @Bean("tossBillingRestClient")
    public RestClient tossBillingRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.tosspayments.com")
                .build();
    }
}
