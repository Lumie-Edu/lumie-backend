package com.lumie.app.config;

import com.lumie.common.tenant.RlsTenantTransactionBinder;
import com.lumie.common.tenant.TenantContextHolder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
@Testcontainers
@Timeout(value = 90, unit = TimeUnit.SECONDS)
class RlsTenantTransactionBinderIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @Test
    void requiresNewTransactionBindingIsVisibleOnlyInsideCurrentTransaction() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(POSTGRES.getJdbcUrl());
        hikariConfig.setUsername(POSTGRES.getUsername());
        hikariConfig.setPassword(POSTGRES.getPassword());
        hikariConfig.setMaximumPoolSize(2);
        hikariConfig.setConnectionTimeout(10_000);

        try (HikariDataSource dataSource = new HikariDataSource(hikariConfig)) {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
            TransactionTemplate outerTransaction = new TransactionTemplate(transactionManager);
            TransactionTemplate requiresNewTransaction = new TransactionTemplate(transactionManager);
            requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            RlsTenantTransactionBinder binder = new RlsTenantTransactionBinder(dataSource);

            TransactionObservation observation = TenantContextHolder.withinContext("outer", 99L, () ->
                    outerTransaction.execute(outerStatus -> {
                        binder.bindRequiredTenantToCurrentTransaction();
                        String outerBefore = currentTenantSetting(jdbcTemplate);
                        String inner = TenantContextHolder.withinContext("inner", 42L, () ->
                                requiresNewTransaction.execute(innerStatus -> {
                                    binder.bindRequiredTenantToCurrentTransaction();
                                    return currentTenantSetting(jdbcTemplate);
                                })
                        );
                        String outerAfter = currentTenantSetting(jdbcTemplate);
                        return new TransactionObservation(outerBefore, inner, outerAfter);
                    })
            );

            assertThat(observation.outerBefore()).isEqualTo("99");
            assertThat(observation.inner()).isEqualTo("42");
            assertThat(observation.outerAfter()).isEqualTo("99");

            String afterCommit = outerTransaction.execute(status -> currentTenantSetting(jdbcTemplate));
            assertThat(afterCommit == null || afterCommit.isBlank())
                    .as("tx-local app.tenant_id must be cleared after commit")
                    .isTrue();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void bindingOutsideTransactionFailsFast() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(POSTGRES.getJdbcUrl());
        hikariConfig.setUsername(POSTGRES.getUsername());
        hikariConfig.setPassword(POSTGRES.getPassword());

        try (HikariDataSource dataSource = new HikariDataSource(hikariConfig)) {
            RlsTenantTransactionBinder binder = new RlsTenantTransactionBinder(dataSource);

            assertThatThrownBy(() -> TenantContextHolder.withinContext("test", 42L,
                    binder::bindRequiredTenantToCurrentTransaction))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("requires an active transaction");
        } finally {
            TenantContextHolder.clear();
        }
    }

    private static String currentTenantSetting(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject(
                "SELECT current_setting('app.tenant_id', true)",
                String.class
        );
    }

    private record TransactionObservation(String outerBefore, String inner, String outerAfter) {
    }
}
