package com.lumie.auth.infrastructure.config;

import com.lumie.auth.infrastructure.multitenancy.CurrentTenantIdentifierResolverImpl;
import com.lumie.auth.infrastructure.multitenancy.SchemaMultiTenantConnectionProvider;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * JPA configuration for Hibernate multi-tenancy with SCHEMA strategy.
 */
@Configuration
@RequiredArgsConstructor
public class JpaMultiTenancyConfig {

    private final SchemaMultiTenantConnectionProvider connectionProvider;
    private final CurrentTenantIdentifierResolverImpl tenantIdentifierResolver;

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return (Map<String, Object> hibernateProperties) -> {
            hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
            hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
        };
    }
}
