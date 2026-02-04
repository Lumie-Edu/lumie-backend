package com.lumie.academy.infrastructure.config;

import com.lumie.academy.infrastructure.tenant.TenantConnectionProvider;
import com.lumie.common.tenant.TenantIdentifierResolver;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.lumie.academy.adapter.out.persistence")
public class JpaConfig {

    private final TenantConnectionProvider tenantConnectionProvider;
    private final TenantIdentifierResolver tenantIdentifierResolver;

    public JpaConfig(TenantConnectionProvider tenantConnectionProvider,
                     TenantIdentifierResolver tenantIdentifierResolver) {
        this.tenantConnectionProvider = tenantConnectionProvider;
        this.tenantIdentifierResolver = tenantIdentifierResolver;
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, tenantConnectionProvider);
            hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
        };
    }
}
