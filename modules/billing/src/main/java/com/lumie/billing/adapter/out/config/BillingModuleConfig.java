package com.lumie.billing.adapter.out.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BillingProperties.class)
public class BillingModuleConfig {

    /**
     * ObjectMapper bean for PaymentTransactionLogger JSON serialization.
     * Reuses the Spring-provided ObjectMapper to keep Jackson config centralized.
     */
    @Bean
    public ObjectMapper billingObjectMapper(ObjectMapper objectMapper) {
        return objectMapper;
    }
}
