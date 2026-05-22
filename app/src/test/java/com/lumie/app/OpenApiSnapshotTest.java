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
import org.springframework.test.web.servlet.MockMvc;

/**
 * Producer-side OpenAPI drift gate. Fetches {@code /v3/api-docs} from the full app
 * context and compares it (normalized) to a committed snapshot. A BE controller/DTO
 * change that alters the contract fails this test until the snapshot is regenerated —
 * the explicit, reviewable update that keeps the spec honest. This is the BE half of the
 * codegen drift defense; the FE {@code nextjs-ci} gate is the consumer half (generated
 * client must match the committed openapi.json).
 *
 * <p>Regenerate after an intended contract change:
 * {@code ./gradlew :app:test --tests '*OpenApiSnapshotTest' -DupdateOpenApiSnapshot=true}
 * then review the diff and commit {@code app/src/test/resources/openapi/api-docs.json}.
 *
 * <p>Shares one cached context with the other full-context tests via
 * {@link FullContextTestBase}. The runtime-dependent {@code servers} block is stripped
 * before comparison.
 */
class OpenApiSnapshotTest extends FullContextTestBase {

    private static final Path SNAPSHOT = Path.of("src/test/resources/openapi/api-docs.json");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .enable(SerializationFeature.INDENT_OUTPUT);

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
