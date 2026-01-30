package com.lumie.billing.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.lumie.billing.adapter.out.persistence")
public class JpaConfig {
}
