package com.lumie.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.auth.adapter.out.config.SessionConfig;
import com.lumie.auth.application.service.AuthQueryService;
import com.lumie.auth.domain.vo.AuthSession;
import com.lumie.auth.domain.vo.DeviceCategory;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.student.application.port.out.StudentPersistencePort;
import com.lumie.student.domain.entity.Student;
import jakarta.servlet.http.Cookie;
import org.flywaydb.core.Flyway;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BackendTenantHttpIsolationIntegrationTest extends FullContextTestBase {

    private static final String ROLE = "boundary_http_test";
    private static final String PASSWORD = "boundary_http_test_pw";

    static {
        prepareRestrictedDatabase();
    }

    @DynamicPropertySource
    static void prepareRuntimeCredentials(DynamicPropertyRegistry registry) {
        System.setProperty("lumie.test.db.username", ROLE);
        System.setProperty("lumie.test.db.password", PASSWORD);
        System.setProperty("lumie.test.modulith.schema-initialization.enabled", "false");
        registry.add("lumie.test.db.restricted-role", () -> ROLE);
    }

    @AfterAll
    static void clearRestrictedRuntimeRole() {
        System.clearProperty("lumie.test.db.username");
        System.clearProperty("lumie.test.db.password");
        System.clearProperty("lumie.test.modulith.schema-initialization.enabled");
    }

    @MockitoBean
    AuthQueryService authQueryService;

    @MockitoBean
    OmrStoragePort omrStoragePort;

    private final MockMvc mockMvc;
    private final SessionConfig sessionConfig;
    private final ObjectMapper objectMapper;
    private final StudentPersistencePort students;
    private final JdbcTemplate primaryJdbc;
    private final JdbcTemplate readOnlyJdbc;

    BackendTenantHttpIsolationIntegrationTest(MockMvc mockMvc,
                                              SessionConfig sessionConfig,
                                              ObjectMapper objectMapper,
                                              StudentPersistencePort students,
                                              @Qualifier("primaryDataSource") DataSource primaryDataSource,
                                              @Qualifier("readOnlyDataSource") DataSource readOnlyDataSource) {
        this.mockMvc = mockMvc;
        this.sessionConfig = sessionConfig;
        this.objectMapper = objectMapper;
        this.students = students;
        this.primaryJdbc = new JdbcTemplate(primaryDataSource);
        this.readOnlyJdbc = new JdbcTemplate(readOnlyDataSource);
    }

    @Test
    @DisplayName("ATDD-US1-002:example-001 authenticated tenant boundary fails closed")
    void authenticatedClassListCannotExposeAnotherTenantsRow() throws Exception {
        assertThat(primaryJdbc.queryForObject("SELECT current_user", String.class)).isEqualTo(ROLE);
        assertThat(readOnlyJdbc.queryForObject("SELECT current_user", String.class)).isEqualTo(ROLE);
        String first = listClassNames("session-one", "tenant-one", 1L);
        String second = listClassNames("session-two", "tenant-two", 2L);

        assertThat(first).contains("Tenant one class").doesNotContain("Tenant two class");
        assertThat(second).contains("Tenant two class").doesNotContain("Tenant one class");
    }

    @Test
    void deletionLookupSeesItsTenantOutsideTheCommandTransaction() {
        TenantContextHolder.setTenant("tenant-one");
        TenantContextHolder.setTenantId(1L);
        try {
            assertThat(primaryJdbc.queryForObject("SELECT current_user", String.class)).isEqualTo(ROLE);
            assertThat(students.findAllById(List.of(990101L)))
                    .extracting(Student::getId)
                    .containsExactly(990101L);
            assertThat(students.findById(990101L)).isPresent();

            TenantContextHolder.setTenant("tenant-two");
            TenantContextHolder.setTenantId(2L);
            assertThat(students.findAllById(List.of(990101L))).isEmpty();
            assertThat(students.findById(990101L)).isEmpty();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void omrPresignSeesOnlyTheAuthenticatedTenantsExam() throws Exception {
        Instant now = Instant.now();
        given(authQueryService.findActiveSession("omr-session")).willReturn(Optional.of(new AuthSession(
                "omr-session", 901L, "Owner", "tenant-one", 1L, Role.OWNER, "test",
                DeviceCategory.UNKNOWN, now, now.plusSeconds(3600))));
        given(omrStoragePort.getPresignedPutUrl(anyString(), eq(900)))
                .willReturn("https://storage.example.invalid/upload");

        presign(990201L).andExpect(status().isOk());
        presign(990202L).andExpect(status().isNotFound());
        presign(990999L).andExpect(status().isNotFound());
        verify(omrStoragePort, times(1)).getPresignedPutUrl(anyString(), eq(900));
    }

    private org.springframework.test.web.servlet.ResultActions presign(Long examId) throws Exception {
        return mockMvc.perform(post("/v1/exams/{examId}/results/omr/batch/presign", examId)
                .header("Host", "localhost")
                .header("Origin", "http://localhost")
                .header("Idempotency-Key", "omr-presign-" + examId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fileNames\":[\"sheet.jpg\"]}")
                .cookie(new Cookie(sessionConfig.getCookieName(), "omr-session")));
    }

    private String listClassNames(String sessionId, String tenantSlug, Long tenantId) throws Exception {
        Instant now = Instant.now();
        given(authQueryService.findActiveSession(sessionId)).willReturn(Optional.of(new AuthSession(
                sessionId, 901L, "Owner", tenantSlug, tenantId, Role.OWNER, "test",
                DeviceCategory.UNKNOWN, now, now.plusSeconds(3600))));

        String body = mockMvc.perform(get("/v1/classes")
                        .header("Host", "localhost")
                        .cookie(new Cookie(sessionConfig.getCookieName(), sessionId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode content = objectMapper.readTree(body).path("items");
        assertThat(content.isArray()).isTrue();
        return content.toString();
    }

    private static void prepareRestrictedDatabase() {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("""
                    DO $$ BEGIN
                      IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'lumie_app') THEN
                        CREATE ROLE lumie_app NOSUPERUSER NOBYPASSRLS;
                      END IF;
                      IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'postgres') THEN
                        CREATE ROLE postgres NOSUPERUSER NOBYPASSRLS;
                      END IF;
                    END $$;
                    """);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to prepare migration roles", e);
        }

        var configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration/public")
                .baselineOnMigrate(true)
                .baselineVersion("0");
        configuration.getConfigurationExtension(PostgreSQLConfigurationExtension.class)
                .setTransactionalLock(false);
        configuration.load().migrate();

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE ROLE " + ROLE + " LOGIN PASSWORD '" + PASSWORD
                    + "' NOSUPERUSER NOBYPASSRLS");
            statement.execute("GRANT USAGE ON SCHEMA public TO " + ROLE);
            statement.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO " + ROLE);
            statement.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO " + ROLE);
            statement.execute("INSERT INTO classes (id, name, tenant_id) "
                    + "VALUES (990001, 'Tenant one class', 1), (990002, 'Tenant two class', 2) "
                    + "ON CONFLICT (id) DO NOTHING");
            statement.execute("INSERT INTO users (id, user_login_id, password_hash, name, role, tenant_id) "
                    + "VALUES (990101, 'boundary_student_990101', 'test-hash', 'Boundary Student', 'STUDENT', 1)");
            statement.execute("INSERT INTO students (id, user_id, user_login_id, is_active, tenant_id) "
                    + "VALUES (990101, 990101, 'boundary_student_990101', false, 1)");
            statement.execute("""
                    INSERT INTO exams (id, name, category, grading_type, grade_scale,
                                       total_questions, correct_answers, question_scores, tenant_id, source)
                    VALUES (990201, 'Tenant one exam', 'GRADED', 'ABSOLUTE', 'NINE_GRADE', 1,
                            '{"1":"1"}'::jsonb, '{"1":2}'::jsonb, 1, 'EXAM_MANAGEMENT'),
                           (990202, 'Tenant two exam', 'GRADED', 'ABSOLUTE', 'NINE_GRADE', 1,
                            '{"1":"1"}'::jsonb, '{"1":2}'::jsonb, 2, 'EXAM_MANAGEMENT')
                    ON CONFLICT (id) DO NOTHING
                    """);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to prepare tenant HTTP fixtures", e);
        }
    }
}
