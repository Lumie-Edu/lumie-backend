package com.lumie.tuition.adapter.out.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TuitionProperties.class)
public class TuitionModuleConfig {

    /**
     * Toss exposes a single host for both sandbox and live —
     * {@code https://api.tosspayments.com}. The environment is selected by the
     * secret-key prefix (test_sk_* vs live_sk_*) sent in Basic Auth, not by a
     * separate hostname (no {@code sandbox-api.tosspayments.com} exists).
     */
    @Bean("tossPaymentsRestClient")
    public RestClient tossPaymentsRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.tosspayments.com")
                .build();
    }
}
