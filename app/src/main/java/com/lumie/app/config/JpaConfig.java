package com.lumie.app.config;

import com.lumie.common.tenant.TenantConnectionProvider;
import com.lumie.common.tenant.TenantIdentifierResolver;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Configuration
@EnableJpaAuditing
@EntityScan(basePackages = "com.lumie")
@EnableJpaRepositories(basePackages = "com.lumie")
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

    /**
     * Programmatic transaction template for services that need fine-grained
     * tx boundaries within a single method (e.g. wrapping an external HTTP
     * call between two short DB transactions). Spring Boot does not
     * auto-configure this bean.
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager txManager) {
        return new TransactionTemplate(txManager);
    }

    /**
     * Primary {@link JdbcTemplate} bound to the read-write DataSource (bluemayne).
     *
     * <p>The ai module registers a {@code readOnlyJdbcTemplate} bean for
     * tenant-scoped analytics queries. That bean satisfies Spring Boot's
     * {@code @ConditionalOnMissingBean(JdbcOperations.class)} guard on
     * {@code JdbcTemplateAutoConfiguration}, so the auto-configured
     * primary {@code JdbcTemplate} is never created. Any consumer
     * (e.g. Spring Modulith's JdbcEventPublicationRepository) wiring a
     * single {@code JdbcOperations} would then get the read-only one
     * and fail with {@code permission denied} on writes.
     *
     * <p>Declaring this {@code @Primary} bean restores the expected
     * default and lets the read-only template stay opt-in via
     * {@code @Qualifier("readOnlyJdbcTemplate")}.
     */
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
