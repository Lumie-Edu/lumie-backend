package com.lumie.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * JPA configuration. Multi-tenancy is enforced via Postgres RLS — no
 * Hibernate {@code MultiTenantConnectionProvider} wiring needed. Per-tx
 * tenant context is bound by {@link com.lumie.common.tenant.RlsTenantContextAspect}.
 *
 * <p>{@code @EnableTransactionManagement(order = HIGHEST_PRECEDENCE)}
 * pins Spring's {@code TransactionInterceptor} as the OUTERMOST advice in
 * the AOP chain so any aspect added with a higher (numerically larger)
 * order — notably the RLS aspect — runs INSIDE the active transaction.
 * Without this, an aspect annotated {@code @Order(HIGHEST_PRECEDENCE+1)}
 * would still run before {@code TransactionInterceptor} because Spring
 * Boot auto-configures the latter at {@code LOWEST_PRECEDENCE} by default
 * (you can't be lower than {@code Integer.MAX_VALUE}). Lowering tx
 * interceptor here is the only way to land custom advice inside the tx.
 */
@Configuration
@EntityScan(basePackages = "com.lumie")
@EnableJpaRepositories(basePackages = "com.lumie")
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
public class JpaConfig {

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
