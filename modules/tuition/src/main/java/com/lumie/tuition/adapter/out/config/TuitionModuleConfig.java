package com.lumie.tuition.adapter.out.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(TuitionProperties.class)
@RequiredArgsConstructor
public class TuitionModuleConfig {

    private final TuitionProperties tuitionProperties;

    @Bean("tossPaymentsRestClient")
    public RestClient tossPaymentsRestClient() {
        String baseUrl = tuitionProperties.toss().sandbox()
                ? "https://sandbox-api.tosspayments.com"
                : "https://api.tosspayments.com";
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
