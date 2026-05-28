package com.lumie.billing.application.service;

import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TestContainers Postgres integration test for {@link BillingOperationLockService}.
 *
 * <p>Drives the service against a real {@code public.billing_operation_locks} table
 * created from the V38 migration DDL. Verifies the mutex semantics, isolation
 * (different tenants / operations), and the high-concurrency race scenario.
 */
@Tag("integration")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("BillingOperationLockService")
class BillingOperationLockServiceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private DataSource dataSource;
    private JdbcTemplate jdbcTemplate;
    private BillingOperationLockService lockService;

    private static final Long TENANT_1 = 1L;
    private static final Long TENANT_2 = 2L;
    private static final String SUBSCRIBE = "SUBSCRIBE";
    private static final String CANCEL = "CANCEL";

    @BeforeAll
    void initSchema() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(POSTGRES.getJdbcUrl());
        ds.setUsername(POSTGRES.getUsername());
        ds.setPassword(POSTGRES.getPassword());
        ds.setDriverClassName("org.postgresql.Driver");
        this.dataSource = ds;
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS public.billing_operation_locks (
                    tenant_id        bigint       NOT NULL,
                    operation_type   varchar(50)  NOT NULL,
                    idempotency_key  varchar(128) NOT NULL,
                    acquired_at      timestamptz  NOT NULL DEFAULT now(),
                    CONSTRAINT billing_operation_locks_pkey PRIMARY KEY (tenant_id, operation_type)
                )
                """);
    }

    @BeforeEach
    void setup() {
        jdbcTemplate.execute("DELETE FROM public.billing_operation_locks");
        this.lockService = new BillingOperationLockService(
                jdbcTemplate, new DataSourceTransactionManager(dataSource));
    }

    @Test
    @DisplayName("acquire_thenAcquireSameTenantSameOp_throwsOperationInProgress")
    void acquire_thenAcquireSameTenantSameOp_throwsOperationInProgress() throws Exception {
        CountDownLatch firstAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);

        // Hold the lock in a background thread
        Future<?> holder = Executors.newSingleThreadExecutor().submit(() ->
                lockService.executeExclusively(TENANT_1, SUBSCRIBE, "key-A", () -> {
                    firstAcquired.countDown();
                    try {
                        releaseFirst.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                })
        );

        firstAcquired.await(5, TimeUnit.SECONDS);

        // Second acquire on same (tenant, op) must reject with 409
        assertThatThrownBy(() ->
                lockService.executeExclusively(TENANT_1, SUBSCRIBE, "key-B", () -> "should-not-reach"))
                .isInstanceOf(BillingException.class)
                .satisfies(ex -> assertThat(((BillingException) ex).getErrorCode())
                        .isEqualTo(BillingErrorCode.OPERATION_IN_PROGRESS));

        releaseFirst.countDown();
        holder.get(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("acquire_thenAcquireSameTenantDifferentOp_succeeds")
    void acquire_thenAcquireSameTenantDifferentOp_succeeds() throws Exception {
        CountDownLatch firstAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);

        Future<?> holder = Executors.newSingleThreadExecutor().submit(() ->
                lockService.executeExclusively(TENANT_1, SUBSCRIBE, "key-A", () -> {
                    firstAcquired.countDown();
                    try {
                        releaseFirst.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                })
        );

        firstAcquired.await(5, TimeUnit.SECONDS);

        // Different operation type must NOT be blocked
        AtomicInteger cancelCalled = new AtomicInteger();
        lockService.executeExclusively(TENANT_1, CANCEL, "key-cancel", () -> {
            cancelCalled.incrementAndGet();
            return null;
        });

        assertThat(cancelCalled.get()).isEqualTo(1);

        releaseFirst.countDown();
        holder.get(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("acquire_thenAcquireDifferentTenantSameOp_succeeds")
    void acquire_thenAcquireDifferentTenantSameOp_succeeds() throws Exception {
        CountDownLatch firstAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);

        Future<?> holder = Executors.newSingleThreadExecutor().submit(() ->
                lockService.executeExclusively(TENANT_1, SUBSCRIBE, "key-A", () -> {
                    firstAcquired.countDown();
                    try {
                        releaseFirst.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                })
        );

        firstAcquired.await(5, TimeUnit.SECONDS);

        // Different tenant, same operation — must NOT be blocked
        AtomicInteger tenant2Called = new AtomicInteger();
        lockService.executeExclusively(TENANT_2, SUBSCRIBE, "key-T2", () -> {
            tenant2Called.incrementAndGet();
            return null;
        });

        assertThat(tenant2Called.get()).isEqualTo(1);

        releaseFirst.countDown();
        holder.get(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("acquire_normalCompletion_releasesLock")
    void acquire_normalCompletion_releasesLock() {
        // First acquisition
        lockService.executeExclusively(TENANT_1, SUBSCRIBE, "key-1", () -> "result-1");

        // After completion the lock must be released — re-acquire must succeed
        AtomicInteger secondCalled = new AtomicInteger();
        lockService.executeExclusively(TENANT_1, SUBSCRIBE, "key-2", () -> {
            secondCalled.incrementAndGet();
            return null;
        });

        assertThat(secondCalled.get()).isEqualTo(1);

        // Confirm no rows remain
        int remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.billing_operation_locks WHERE tenant_id = ? AND operation_type = ?",
                Integer.class, TENANT_1, SUBSCRIBE);
        assertThat(remaining).isZero();
    }

    @Test
    @DisplayName("acquire_actionThrows_releasesLock")
    void acquire_actionThrows_releasesLock() {
        // Simulate an action that throws
        assertThatThrownBy(() ->
                lockService.executeExclusively(TENANT_1, SUBSCRIBE, "key-err", () -> {
                    throw new IllegalStateException("action failed");
                })
        ).isInstanceOf(IllegalStateException.class).hasMessage("action failed");

        // Lock must still be released by the finally block
        int remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM public.billing_operation_locks WHERE tenant_id = ? AND operation_type = ?",
                Integer.class, TENANT_1, SUBSCRIBE);
        assertThat(remaining).isZero();

        // Re-acquire must succeed
        AtomicInteger retried = new AtomicInteger();
        lockService.executeExclusively(TENANT_1, SUBSCRIBE, "key-retry", () -> {
            retried.incrementAndGet();
            return null;
        });
        assertThat(retried.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("acquire_concurrentRace_only_one_succeeds — 100 threads, exactly 1 holds while 99 get 409")
    void acquire_concurrentRace_only_one_succeeds() throws Exception {
        int competitorCount = 99;
        // Holder signals once it has acquired the lock and is inside the action.
        CountDownLatch holderInside = new CountDownLatch(1);
        // Holder waits until all 99 competitors have finished (succeeded or 409).
        CountDownLatch allCompetitorsDone = new CountDownLatch(competitorCount);
        // We release the holder once competitors are tallied.
        CountDownLatch releaseHolder = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(competitorCount + 1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            // Holder thread: acquires lock and holds until all competitors have attempted.
            futures.add(pool.submit(() -> {
                lockService.executeExclusively(TENANT_1, SUBSCRIBE, "holder-key", () -> {
                    holderInside.countDown();
                    try {
                        releaseHolder.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    successCount.incrementAndGet();
                    return null;
                });
                return null;
            }));

            // Wait until holder holds the lock before unleashing competitors.
            assertThat(holderInside.await(10, TimeUnit.SECONDS))
                    .as("holder must acquire lock within 10s").isTrue();

            // 99 competitor threads all attempt the same (tenant, op) while lock is held.
            for (int i = 0; i < competitorCount; i++) {
                final String key = "competitor-key-" + i;
                futures.add(pool.submit(() -> {
                    try {
                        lockService.executeExclusively(TENANT_1, SUBSCRIBE, key, () -> {
                            successCount.incrementAndGet();
                            return null;
                        });
                    } catch (BillingException ex) {
                        if (ex.getErrorCode() == BillingErrorCode.OPERATION_IN_PROGRESS) {
                            conflictCount.incrementAndGet();
                        }
                    } finally {
                        allCompetitorsDone.countDown();
                    }
                    return null;
                }));
            }

            assertThat(allCompetitorsDone.await(20, TimeUnit.SECONDS))
                    .as("all competitors must finish within 20s").isTrue();
            releaseHolder.countDown();

            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(successCount.get())
                .as("exactly one thread must acquire the lock")
                .isEqualTo(1);
        assertThat(conflictCount.get())
                .as("all competitors must receive OPERATION_IN_PROGRESS")
                .isEqualTo(competitorCount);
    }
}
