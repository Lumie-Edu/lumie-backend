package com.lumie.tenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Tenant Service Application.
 * Manages tenant lifecycle, schema provisioning, and tenant settings.
 */
@SpringBootApplication
@EnableJpaAuditing
public class TenantApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantApplication.class, args);
    }
}
