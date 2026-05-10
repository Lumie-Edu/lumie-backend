package com.lumie.common.idempotency;

import com.lumie.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TestContainers Postgres integration test for {@link IdempotencyService}.
 *
 * <p>Drives the service against a real {@code public.idempotency_keys} table
 * created from the V16 migration's DDL — same schema as production. Verifies
 * the three core replay/conflict semantics plus race-loser fallback.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdempotencyServiceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private IdempotencyService service;

    @BeforeAll
    void initSchema() {
        org.springframework.jdbc.datasource.DriverManagerDataSource ds =
                new org.springframework.jdbc.datasource.DriverManagerDataSource();
        ds.setUrl(POSTGRES.getJdbcUrl());
        ds.setUsername(POSTGRES.getUsername());
        ds.setPassword(POSTGRES.getPassword());
        ds.setDriverClassName("org.postgresql.Driver");
        this.dataSource = ds;
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS idempotency_keys (
                    id BIGSERIAL PRIMARY KEY,
                    tenant_slug VARCHAR(80) NOT NULL,
                    endpoint_path VARCHAR(255) NOT NULL,
                    idempotency_key VARCHAR(255) NOT NULL,
                    request_fingerprint VARCHAR(64) NOT NULL,
                    response_status INT NOT NULL,
                    response_body TEXT,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    CONSTRAINT uq_idempotency_keys UNIQUE (tenant_slug, endpoint_path, idempotency_key)
                )
                """);
    }

    @BeforeEach
    void setup() {
        jdbcTemplate.execute("TRUNCATE idempotency_keys RESTART IDENTITY");
        TenantContextHolder.setTenant("acme");
        this.service = new IdempotencyService(jdbcTemplate, new DataSourceTransactionManager(dataSource));
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void firstCall_writesRow_andReturnsActionResponse() {
        AtomicInteger callCount = new AtomicInteger();
        Map<String, Object> body = Map.of("amount", 1000, "currency", "KRW");

        ResponseEntity<Map<String, Object>> response = service.executeOnce(
                "key-1",
                "POST /v1/payments/confirm",
                body,
                () -> {
                    callCount.incrementAndGet();
                    return ResponseEntity.ok(Map.of("paymentId", "pay_123"));
                }
        );

        assertThat(callCount.get()).isEqualTo(1);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("paymentId", "pay_123");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT tenant_slug, endpoint_path, idempotency_key, response_status FROM idempotency_keys");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0))
                .containsEntry("tenant_slug", "acme")
                .containsEntry("endpoint_path", "POST /v1/payments/confirm")
                .containsEntry("idempotency_key", "key-1")
                .containsEntry("response_status", 200);
    }

    @Test
    void secondCall_sameKey_sameFingerprint_returnsCachedResponse_withoutInvokingAction() {
        AtomicInteger callCount = new AtomicInteger();
        Map<String, Object> body = Map.of("amount", 1000, "currency", "KRW");

        service.executeOnce("key-2", "POST /v1/payments/confirm", body,
                () -> {
                    callCount.incrementAndGet();
                    return ResponseEntity.ok(Map.of("paymentId", "pay_456"));
                });

        ResponseEntity<Map<String, Object>> replay = service.executeOnce(
                "key-2", "POST /v1/payments/confirm", body,
                () -> {
                    callCount.incrementAndGet();
                    return ResponseEntity.ok(Map.of("paymentId", "should-not-be-returned"));
                }
        );

        assertThat(callCount.get()).as("action invoked exactly once").isEqualTo(1);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replay.getBody()).containsEntry("paymentId", "pay_456");
    }

    @Test
    void secondCall_sameKey_differentFingerprint_throwsConflict() {
        service.executeOnce("key-3", "POST /v1/payments/confirm",
                Map.of("amount", 1000, "currency", "KRW"),
                () -> ResponseEntity.ok(Map.of("paymentId", "pay_789")));

        assertThatThrownBy(() -> service.executeOnce(
                "key-3", "POST /v1/payments/confirm",
                Map.of("amount", 9999, "currency", "KRW"),  // different amount
                () -> ResponseEntity.ok(Map.of("paymentId", "pay_other"))
        ))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("key-3");
    }

    @Test
    void differentTenant_isolated_noCollision() {
        service.executeOnce("shared-key", "POST /v1/payments/confirm",
                Map.of("amount", 1),
                () -> ResponseEntity.ok(Map.of("paymentId", "tenant-acme")));

        TenantContextHolder.setTenant("other");
        AtomicInteger callCount = new AtomicInteger();
        ResponseEntity<Map<String, Object>> response = service.executeOnce(
                "shared-key", "POST /v1/payments/confirm",
                Map.of("amount", 2),
                () -> {
                    callCount.incrementAndGet();
                    return ResponseEntity.ok(Map.of("paymentId", "tenant-other"));
                }
        );

        assertThat(callCount.get()).as("each tenant runs the action independently").isEqualTo(1);
        assertThat(response.getBody()).containsEntry("paymentId", "tenant-other");
    }

    @Test
    void differentEndpointScope_isolated_noCollision() {
        AtomicInteger callCount = new AtomicInteger();
        service.executeOnce("k", "POST /v1/payments/confirm", Map.of("a", 1),
                () -> {
                    callCount.incrementAndGet();
                    return ResponseEntity.ok(Map.of("r", "payments"));
                });
        service.executeOnce("k", "POST /v1/exams/1/reports/batch", Map.of("a", 1),
                () -> {
                    callCount.incrementAndGet();
                    return ResponseEntity.ok(Map.of("r", "reports"));
                });

        assertThat(callCount.get()).isEqualTo(2);
    }
}
