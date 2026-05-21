package com.lumie.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Smoke test: verifies the full Spring context can be refreshed starting from
 * {@link LumieApplication} (scanBasePackages = "com.lumie").
 *
 * <p><b>Why this exists.</b> On 2026-05-21 {@code StaffRepositoryCustomImpl}
 * lived in {@code adapter/out/persistence/} while its fragment interface
 * {@code StaffRepositoryCustom} lived in {@code domain/repository/}. Spring
 * Data's default fragment-impl scan walks the interface's package, so the impl
 * was never found; the repository proxy fell back to query derivation and
 * exploded at boot with "No property 'filter' found for type 'Staff'". The
 * existing slice tests (which restrict {@code @EnableJpaRepositories} to an
 * empty package) could not catch this class of failure. This test catches it
 * because it loads the full {@code @SpringBootApplication} scan — repository
 * proxy construction across ALL modules is performed at context refresh time.
 *
 * <p><b>Design.</b> Module {@code @Configuration} classes inside
 * {@code com.lumie.*} construct application beans (Redis, Rabbit, Spring AI)
 * from autoconfigured factories. Excluding those autoconfiguration classes
 * breaks the module configs too. Instead, real TestContainers instances are
 * provided for Postgres, Redis, and RabbitMQ so all factories resolve. Spring
 * AI just needs a non-empty API key (set via {@code @DynamicPropertySource}).
 *
 * <p>RabbitMQ note: the exam module's custom listener factory takes
 * {@code lumie.rabbitmq.missing-queues-fatal} (default {@code true}) so this
 * test can flip it to {@code false} via {@code @DynamicPropertySource} without
 * bean-definition override gymnastics. Queues are declared by the RabbitMQ
 * Topology Operator in {@code lumie-infra}, not by Spring beans, so on a fresh
 * test container they don't exist and the listener must retry silently rather
 * than crash context refresh.
 *
 * <p><b>Flyway is disabled.</b> Running all migrations from scratch on a fresh
 * TestContainers PG deadlocks: Flyway's schema_history connection holds an
 * idle-in-transaction state while {@code CREATE INDEX CONCURRENTLY} in a
 * sibling Hikari connection waits for all active transactions to clear.
 * {@code spring.jpa.hibernate.ddl-auto=none} lets the EMF bean graph boot
 * without schema validation. ShedLock ({@code public.shedlock} table) and
 * Spring Modulith outbox ({@code public.event_publication} table) are also
 * disabled because their tables are Flyway-managed and do not exist on a
 * blank PG instance.
 *
 * <p><b>No {@code @Tag("integration")}.</b> This test must run in the default
 * {@code ./gradlew test} so it gates Tekton's gradle-test step.
 */
@SpringBootTest(
        classes = LumieApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Testcontainers
class ApplicationContextLoadsTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> RABBITMQ =
            new GenericContainer<>(DockerImageName.parse("rabbitmq:3-alpine"))
                    .withExposedPorts(5672)
                    .withEnv("RABBITMQ_DEFAULT_USER", "guest")
                    .withEnv("RABBITMQ_DEFAULT_PASS", "guest");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // --- DataSource: both pools target the same container ---
        registry.add("app.datasource.primary.url", POSTGRES::getJdbcUrl);
        registry.add("app.datasource.primary.username", POSTGRES::getUsername);
        registry.add("app.datasource.primary.password", POSTGRES::getPassword);
        registry.add("app.datasource.readonly.url", POSTGRES::getJdbcUrl);
        registry.add("app.datasource.readonly.username", POSTGRES::getUsername);
        registry.add("app.datasource.readonly.password", POSTGRES::getPassword);

        // --- Flyway: disabled (CREATE INDEX CONCURRENTLY deadlock on fresh PG) ---
        registry.add("spring.flyway.enabled", () -> "false");
        // JPA: no schema validation required for this smoke test
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");

        // --- Redis: point at the container ---
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379).toString());
        registry.add("spring.data.redis.password", () -> "");

        // --- RabbitMQ: point at the container ---
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBITMQ.getMappedPort(5672).toString());
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("spring.rabbitmq.virtual-host", () -> "/");

        // --- Spring Modulith outbox: disable (needs event_publication table from Flyway V13) ---
        registry.add("spring.modulith.events.republish-outstanding-events-on-restart", () -> "false");

        // --- RabbitMQ listeners: queues are declared via infra RabbitMQ Topology Operator,
        //     not via Spring @Configuration. On a fresh container they don't exist.
        //     The custom rabbitListenerContainerFactory in RabbitMqConfig reads
        //     lumie.rabbitmq.missing-queues-fatal; flipping it false lets the listener
        //     containers retry silently instead of crashing context refresh.
        //     (The spring.rabbitmq.listener.* properties don't apply because we provide
        //     our own factory bean — they only configure Spring Boot's auto-factory.) ---
        registry.add("lumie.rabbitmq.missing-queues-fatal", () -> "false");

        // --- Spring Security: no web layer needed, suppress mandatory UserDetailsService ---
        registry.add("spring.autoconfigure.exclude", () -> String.join(",",
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
                "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration",
                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
        ));

        // --- Required @Value / @ConfigurationProperties fields that have no defaults ---
        registry.add("jwt.secret-key",
                () -> "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm");
        registry.add("jwt.access-expiration", () -> "3600");
        registry.add("jwt.refresh-expiration", () -> "604800");
        registry.add("jwt.issuer", () -> "lumie-test");
        registry.add("cookie.domain", () -> "localhost");
        registry.add("cookie.secure", () -> "false");
        registry.add("lumie.internal.hmac-secret", () -> "test-hmac-secret-for-context-loads");
        registry.add("lumie.services.grading.url", () -> "http://localhost:8001");
        registry.add("lumie.services.report.url", () -> "http://localhost:8002");
        registry.add("spring.ai.openai.api-key", () -> "test-dummy-key");
        registry.add("telegram.bot-token", () -> "test-token");
        registry.add("paddle.api-key", () -> "test-key");
        registry.add("paddle.webhook-secret", () -> "test-secret");
        registry.add("minio.endpoint", () -> "http://localhost:9000");
        registry.add("minio.external-endpoint", () -> "http://localhost:9000");
        registry.add("minio.access-key", () -> "test");
        registry.add("minio.secret-key", () -> "test");
    }

    /**
     * The entire value of this test is the context refresh phase above.
     * If Spring Data cannot wire all repository proxies (e.g. missing
     * {@code @Component} on a cross-package fragment impl), the context
     * refresh throws before this method body is ever reached.
     */
    @Test
    void contextLoads() {
        // intentionally empty — failure = context did not refresh
    }
}
