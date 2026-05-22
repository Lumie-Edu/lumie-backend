package com.lumie.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Producer-side OpenAPI drift gate. Boots the full app context, fetches
 * {@code /v3/api-docs}, and compares it (normalized) to a committed snapshot. A BE
 * controller/DTO change that alters the contract fails this test until the snapshot
 * is regenerated — the explicit, reviewable update that keeps the spec honest. This
 * is the BE half of the codegen drift defense; the FE {@code nextjs-ci} gate is the
 * consumer half (generated client must match the committed openapi.json).
 *
 * <p>Regenerate after an intended contract change:
 * {@code ./gradlew :app:test --tests '*OpenApiSnapshotTest' -DupdateOpenApiSnapshot=true}
 * then review the diff and commit {@code app/src/test/resources/openapi/api-docs.json}.
 *
 * <p>Boot setup mirrors {@link ApplicationContextLoadsTest} (real TestContainers for
 * Postgres/Redis/RabbitMQ, Flyway off, missing-queue listeners non-fatal). Web env is
 * MOCK so MockMvc can hit the springdoc endpoint, which {@code SecurityConfig}
 * permits. The runtime-dependent {@code servers} block is stripped before comparison.
 */
@SpringBootTest(classes = LumieApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class OpenApiSnapshotTest {

    private static final Path SNAPSHOT = Path.of("src/test/resources/openapi/api-docs.json");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .enable(SerializationFeature.INDENT_OUTPUT);

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
        registry.add("lumie.rabbitmq.missing-queues-fatal", () -> "false");

        registry.add("spring.autoconfigure.exclude", () -> String.join(",",
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
                "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration",
                "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"
        ));

        registry.add("jwt.secret-key",
                () -> "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm");
        registry.add("jwt.access-expiration", () -> "3600");
        registry.add("jwt.refresh-expiration", () -> "604800");
        registry.add("jwt.issuer", () -> "lumie-test");
        registry.add("cookie.domain", () -> "localhost");
        registry.add("cookie.secure", () -> "false");
        registry.add("lumie.internal.hmac-secret", () -> "test-hmac-secret-for-openapi-snapshot");
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

    private final MockMvc mockMvc;

    OpenApiSnapshotTest(@Autowired MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void specMatchesCommittedSnapshot() throws Exception {
        final String raw = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        final String current = normalize(raw);

        if (Boolean.getBoolean("updateOpenApiSnapshot") || Files.notExists(SNAPSHOT)) {
            Files.createDirectories(SNAPSHOT.getParent());
            Files.writeString(SNAPSHOT, current, StandardCharsets.UTF_8);
            return;
        }

        final String committed = normalize(Files.readString(SNAPSHOT, StandardCharsets.UTF_8));
        assertThat(current)
                .as("OpenAPI contract drifted from the committed snapshot. Review the change; if "
                        + "intended, regenerate with `./gradlew :app:test --tests '*OpenApiSnapshotTest' "
                        + "-DupdateOpenApiSnapshot=true` and commit app/src/test/resources/openapi/api-docs.json.")
                .isEqualTo(committed);
    }

    /**
     * Canonical JSON, minus the runtime-dependent {@code servers} block. Reads into a
     * {@code Map} (not a {@code JsonNode}) so {@code ORDER_MAP_ENTRIES_BY_KEYS} actually
     * sorts every object's keys recursively — springdoc emits some property maps in a
     * non-deterministic order across boots, which would otherwise cause false drift.
     */
    @SuppressWarnings("unchecked")
    private static String normalize(String json) throws Exception {
        final Map<String, Object> root = MAPPER.readValue(json, Map.class);
        root.remove("servers");
        return MAPPER.writeValueAsString(root);
    }
}
