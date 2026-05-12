package com.lumie.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end verification that {@link RlsTenantContextAspect} actually
 * runs INSIDE Spring's transaction interceptor so the
 * {@code set_config('app.tenant_id', ?, true)} call lands on a connection
 * whose {@code autoCommit} is {@code false} — the precondition for
 * tx-local GUCs to persist.
 *
 * <p>This test exists because of post-mortem entry #22: an earlier
 * mechanism set the tenant context BEFORE the tx had begun, so the
 * setting evaporated after a single statement. The aspect-ordering trick
 * ({@code @EnableTransactionManagement(order = HIGHEST_PRECEDENCE)} +
 * {@code @Order(HIGHEST_PRECEDENCE + 1)}) is the load-bearing
 * configuration; this test will catch any future regression of that
 * invariant.
 */
@SpringBootTest(
        classes = RlsTenantContextAspectIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class RlsTenantContextAspectIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private final RlsProbeService probeService;

    RlsTenantContextAspectIntegrationTest(RlsProbeService probeService) {
        this.probeService = probeService;
    }

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
    }

    @Test
    void setsTenantIdInsideTransaction_whenContextIsPopulated() {
        TenantContextHolder.setTenantId(42L);

        String observed = probeService.readTenantSettingInsideTransaction();

        assertThat(observed).isEqualTo("42");
    }

    @Test
    void doesNotSetTenantId_whenContextIsEmpty() {
        // No setTenantId — aspect should no-op
        String observed = probeService.readTenantSettingInsideTransaction();

        // Postgres returns empty string for an unset GUC with the
        // missing_ok=true flag (true) in current_setting(..., true)
        assertThat(observed).isEmpty();
    }

    @Test
    void tenantIdSettingDoesNotLeakAcrossTransactions() {
        TenantContextHolder.setTenantId(99L);
        String first = probeService.readTenantSettingInsideTransaction();
        assertThat(first).isEqualTo("99");

        // Clear context, run another tx — should see no setting
        TenantContextHolder.clear();
        String second = probeService.readTenantSettingInsideTransaction();
        assertThat(second).isEmpty();
    }

    @Configuration
    @EnableAspectJAutoProxy
    @EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setUrl(POSTGRES.getJdbcUrl());
            ds.setUsername(POSTGRES.getUsername());
            ds.setPassword(POSTGRES.getPassword());
            ds.setDriverClassName("org.postgresql.Driver");
            return ds;
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager tm) {
            return new TransactionTemplate(tm);
        }

        @Bean
        RlsTenantContextAspect rlsTenantContextAspect(DataSource dataSource) {
            return new RlsTenantContextAspect(dataSource);
        }

        @Bean
        RlsProbeService rlsProbeService(JdbcTemplate jdbcTemplate) {
            return new RlsProbeService(jdbcTemplate);
        }
    }

    @Component
    static class RlsProbeService {
        private final JdbcTemplate jdbcTemplate;

        RlsProbeService(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Transactional
        public String readTenantSettingInsideTransaction() {
            String value = jdbcTemplate.queryForObject(
                    "SELECT current_setting('app.tenant_id', true)", String.class);
            return value == null ? "" : value;
        }
    }
}
