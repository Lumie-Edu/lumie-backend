package com.lumie.student.adapter.out.persistence;

import com.lumie.auth.api.AuthService;
import com.lumie.billing.api.BillingService;
import com.lumie.classroom.api.ClassService;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.student.application.port.out.StudentPersistencePort;
import com.lumie.student.application.service.StudentCommandService;
import com.lumie.student.domain.entity.Student;
import com.lumie.student.domain.exception.StudentErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@Tag("integration")
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DataJpaTest(
        excludeAutoConfiguration = FlywayAutoConfiguration.class,
        properties = "spring.jpa.hibernate.ddl-auto=create"
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = StudentDeletionIntegrationTest.JpaTestConfiguration.class)
@Import({StudentPersistenceAdapter.class, StudentCommandService.class})
class StudentDeletionIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean
    AuthService authService;

    @MockitoBean
    BillingService billingService;

    @MockitoBean
    ClassService classService;

    private final StudentPersistencePort students;
    private final StudentCommandService commands;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;

    StudentDeletionIntegrationTest(StudentPersistencePort students,
                                   StudentCommandService commands,
                                   EntityManager entityManager,
                                   JdbcTemplate jdbc) {
        this.students = students;
        this.commands = commands;
        this.entityManager = entityManager;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void prepare() {
        TenantContextHolder.setTenant("student-it");
        TenantContextHolder.setTenantId(1L);
        jdbc.execute("CREATE TABLE IF NOT EXISTS users (id BIGINT PRIMARY KEY, name VARCHAR(255), phone VARCHAR(32))");
        jdbc.update("""
                INSERT INTO users (id, name, phone)
                VALUES (10, 'Student', '01011111111'),
                       (11, 'Student Two', '01022222222'),
                       (12, 'Tenant Student', '01033333333')
                ON CONFLICT (id) DO NOTHING
                """);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failedLinkedUserDeletionIsNotReportedAsSuccessOrRemovedFromDatabase() {
        Student inactive = Student.create(10L, "student_10", "Student",
                "01011111111", null, null, null, null);
        inactive.deactivate();
        Student saved = students.save(inactive);
        given(authService.deleteUser(10L))
                .willReturn(new AuthService.DeleteUserResult(false, "auth unavailable"));

        assertThatThrownBy(() -> commands.deleteStudent(saved.getId()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(StudentErrorCode.AUTH_OP_FAILED));
        assertThat(students.findById(saved.getId())).isPresent();
        verify(classService).dropActiveEnrollmentsForStudent("student-it", saved.getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void batchDeletionCountsOnlySuccessfulLinkedUserOutcomes() {
        Student inactive = Student.create(11L, "student_11", "Student Two",
                "01022222222", null, null, null, null);
        inactive.deactivate();
        Student saved = students.save(inactive);
        given(authService.deleteUser(11L))
                .willReturn(new AuthService.DeleteUserResult(true, "ok"));

        var result = commands.batchDelete(List.of(saved.getId(), 999L));

        assertThat(result.success()).isEqualTo(1);
        assertThat(result.failures()).extracting(failure -> failure.id()).containsExactly(999L);
        verify(authService).deleteUser(11L);
    }

    @Test
    void foreignAndMissingTenantCannotReadStudentThroughAdapter() {
        jdbc.execute("ALTER TABLE students ENABLE ROW LEVEL SECURITY");
        jdbc.execute("ALTER TABLE students FORCE ROW LEVEL SECURITY");
        jdbc.execute("""
                CREATE POLICY tenant_isolation ON students
                USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
                WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
                """);
        jdbc.execute("CREATE ROLE student_rls_test NOSUPERUSER NOBYPASSRLS");
        jdbc.execute("GRANT USAGE ON SCHEMA public TO student_rls_test");
        jdbc.execute("GRANT SELECT ON users TO student_rls_test");
        jdbc.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON students TO student_rls_test");
        jdbc.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO student_rls_test");
        jdbc.execute("SET LOCAL ROLE student_rls_test");
        jdbc.execute("SET LOCAL app.tenant_id = '1'");
        assertThat(jdbc.queryForObject("SELECT current_user", String.class)).isEqualTo("student_rls_test");

        Student saved = students.save(Student.create(12L, "student_12", "Tenant Student",
                "01033333333", null, null, null, null));
        entityManager.flush();
        entityManager.clear();

        jdbc.execute("SET LOCAL app.tenant_id = '2'");
        assertThat(students.findById(saved.getId())).isEmpty();
        jdbc.execute("SET LOCAL app.tenant_id = ''");
        assertThat(students.findById(saved.getId())).isEmpty();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Student.class)
    @EnableJpaRepositories(basePackageClasses = StudentJpaRepository.class)
    @EnableJpaAuditing
    static class JpaTestConfiguration {
    }
}
