package com.lumie.app;

import org.junit.jupiter.api.Test;

/**
 * Smoke test: verifies the full Spring context can be refreshed starting from
 * {@link LumieApplication} (scanBasePackages = "com.lumie").
 *
 * <p><b>Why this exists.</b> On 2026-05-21 {@code StaffRepositoryCustomImpl} lived in
 * {@code adapter/out/persistence/} while its fragment interface {@code StaffRepositoryCustom}
 * lived in {@code domain/repository/}. Spring Data's default fragment-impl scan walks the
 * interface's package, so the impl was never found; the repository proxy fell back to query
 * derivation and exploded at boot. Slice tests (which restrict {@code @EnableJpaRepositories}
 * to an empty package) cannot catch this; this test loads the full {@code @SpringBootApplication}
 * scan, so repository-proxy construction across ALL modules happens at context refresh time.
 *
 * <p>Boot wiring (TestContainers for Postgres/Redis/RabbitMQ, Flyway off, missing-queue
 * listeners non-fatal, security autoconfig excluded) lives in {@link FullContextTestBase},
 * shared with {@link OpenApiSnapshotTest} so both reuse a single cached context.
 *
 * <p><b>No {@code @Tag("integration")}.</b> This test must run in the default
 * {@code ./gradlew test} so it gates Tekton's gradle-test step.
 */
class ApplicationContextLoadsTest extends FullContextTestBase {

    /**
     * The entire value of this test is the context refresh inherited from
     * {@link FullContextTestBase}. If Spring Data cannot wire all repository proxies, the
     * context refresh throws before this method body is ever reached.
     */
    @Test
    void contextLoads() {
        // intentionally empty — failure = context did not refresh
    }
}
