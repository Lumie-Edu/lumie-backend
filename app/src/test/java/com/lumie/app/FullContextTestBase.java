package com.lumie.app;

import java.time.Duration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared base for the app module's full-{@code @SpringBootApplication}-context tests
 * ({@link ApplicationContextLoadsTest}, {@link OpenApiSnapshotTest}). Declaring the
 * {@code @SpringBootTest} configuration once means Spring's test-context cache hands
 * the SAME refreshed context to every subclass — a single boot and a single set of
 * TestContainers, instead of one full monolith context per test class. That matters:
 * two coexisting full contexts in the {@code :app:test} JVM exceeded the CI step's
 * memory limit and got OOM-killed.
 *
 * <p>Web env is MOCK (not NONE) so subclasses that need {@code MockMvc} (the springdoc
 * {@code /v3/api-docs} snapshot) can inject it; a context-only smoke test simply ignores
 * it. The full context still refreshes, so repository-proxy wiring across all modules is
 * exercised exactly as before.
 *
 * <p>Real TestContainers back Postgres/Redis/RabbitMQ so every module {@code @Configuration}
 * resolves. Flyway is off (its {@code CREATE INDEX CONCURRENTLY} deadlocks on a fresh PG)
 * with {@code ddl-auto=none}; RabbitMQ listeners are not auto-started; Spring Security
 * autoconfig is excluded (the app's own {@code @EnableWebSecurity SecurityConfig} provides
 * the filter chain and permits {@code /v3/api-docs}).
 */
@SpringBootTest(classes = LumieApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class FullContextTestBase {

    private static final Duration CONTAINER_STARTUP_TIMEOUT = Duration.ofMinutes(3);

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withStartupTimeout(CONTAINER_STARTUP_TIMEOUT);

    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379)
                    .withStartupTimeout(CONTAINER_STARTUP_TIMEOUT);

    @SuppressWarnings("resource")
    static final GenericContainer<?> RABBITMQ =
            new GenericContainer<>(DockerImageName.parse("rabbitmq:3-alpine"))
                    .withExposedPorts(5672)
                    .withEnv("RABBITMQ_DEFAULT_USER", "guest")
                    .withEnv("RABBITMQ_DEFAULT_PASS", "guest")
                    .withStartupTimeout(CONTAINER_STARTUP_TIMEOUT);

    static {
        Startables.deepStart(POSTGRES, REDIS, RABBITMQ).join();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("app.datasource.primary.url", POSTGRES::getJdbcUrl);
        registry.add("app.datasource.primary.username", POSTGRES::getUsername);
        registry.add("app.datasource.primary.password", POSTGRES::getPassword);
        registry.add("app.datasource.readonly.url", POSTGRES::getJdbcUrl);
        registry.add("app.datasource.readonly.username", POSTGRES::getUsername);
        registry.add("app.datasource.readonly.password", POSTGRES::getPassword);

        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379).toString());
        registry.add("spring.data.redis.password", () -> "");

        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBITMQ.getMappedPort(5672).toString());
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        registry.add("spring.rabbitmq.virtual-host", () -> "/");

        registry.add("spring.modulith.events.republish-outstanding-events-on-restart", () -> "false");
        registry.add("spring.modulith.events.jdbc.schema-initialization.enabled", () -> "true");
        registry.add("lumie.rabbitmq.missing-queues-fatal", () -> "false");
        registry.add("lumie.rabbitmq.listener-auto-startup", () -> "false");

        // TestContainers provisions a superuser — the guard is a deployed-env safeguard.
        // Disable it here so full-context tests are not blocked by the superuser role.
        registry.add("lumie.security.runtime-role-guard.enabled", () -> "false");

        registry.add("spring.autoconfigure.exclude", () -> String.join(",",
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
                "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration",
                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
        ));

        registry.add("session.expiration", () -> "604800");
        registry.add("tenant.edge.root-hosts", () -> "lumie-edu.com,localhost");
        registry.add("lumie.internal.hmac-secret", () -> "test-hmac-secret-for-full-context-tests");
        registry.add("lumie.services.grading.url", () -> "http://localhost:8001");
        registry.add("lumie.services.report.url", () -> "http://localhost:8002");
        registry.add("telegram.bot-token", () -> "test-token");
        registry.add("paddle.api-key", () -> "test-key");
        registry.add("paddle.webhook-secret", () -> "test-secret");
        registry.add("minio.endpoint", () -> System.getProperty("lumie.test.minio.endpoint", "http://localhost:9000"));
        registry.add("minio.external-endpoint", () -> System.getProperty(
                "lumie.test.minio.external-endpoint", "http://localhost:9000"));
        registry.add("minio.access-key", () -> System.getProperty("lumie.test.minio.access-key", "test"));
        registry.add("minio.secret-key", () -> System.getProperty("lumie.test.minio.secret-key", "test"));
    }
}
