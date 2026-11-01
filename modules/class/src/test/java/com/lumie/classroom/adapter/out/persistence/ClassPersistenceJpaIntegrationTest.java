package com.lumie.classroom.adapter.out.persistence;

import com.lumie.classroom.application.port.out.ClassEnrollmentPersistencePort;
import com.lumie.classroom.application.port.out.ClassPersistencePort;
import com.lumie.classroom.domain.entity.ClassEnrollment;
import com.lumie.classroom.domain.entity.ClassEntity;
import com.lumie.classroom.domain.vo.EnrollmentStatus;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DataJpaTest(
        excludeAutoConfiguration = FlywayAutoConfiguration.class,
        properties = "spring.jpa.hibernate.ddl-auto=create"
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ClassPersistenceJpaIntegrationTest.JpaTestConfiguration.class)
class ClassPersistenceJpaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private final ClassPersistencePort classes;
    private final ClassEnrollmentPersistencePort enrollments;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;

    ClassPersistenceJpaIntegrationTest(ClassPersistencePort classes,
                                       ClassEnrollmentPersistencePort enrollments,
                                       EntityManager entityManager,
                                       JdbcTemplate jdbc) {
        this.classes = classes;
        this.enrollments = enrollments;
        this.entityManager = entityManager;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void bindRestrictedTenant() {
        TenantContextHolder.setTenant("class-it");
        TenantContextHolder.setTenantId(1L);
        for (String table : new String[]{"classes", "class_enrollments"}) {
            jdbc.execute("ALTER TABLE " + table + " ENABLE ROW LEVEL SECURITY");
            jdbc.execute("ALTER TABLE " + table + " FORCE ROW LEVEL SECURITY");
            jdbc.execute("CREATE POLICY tenant_isolation ON " + table
                    + " USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)"
                    + " WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)");
        }
        jdbc.execute("CREATE ROLE class_rls_test NOSUPERUSER NOBYPASSRLS");
        jdbc.execute("GRANT USAGE ON SCHEMA public TO class_rls_test");
        jdbc.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON classes, class_enrollments TO class_rls_test");
        jdbc.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO class_rls_test");
        jdbc.execute("SET LOCAL ROLE class_rls_test");
        jdbc.execute("SET LOCAL app.tenant_id = '1'");
        assertThat(jdbc.queryForObject("SELECT current_user", String.class)).isEqualTo("class_rls_test");
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("ATDD-US1-001:example-003 class enrollment lookup")
    void enrolledClassQueryPreservesStatusAndLocking() {
        ClassEntity enrolled = classes.save(ClassEntity.create("Enrolled", 7L, null, null));
        ClassEntity dropped = classes.save(ClassEntity.create("Dropped", 7L, null, null));
        enrollments.save(ClassEnrollment.create(enrolled, 99L));
        ClassEnrollment droppedEnrollment = ClassEnrollment.create(dropped, 99L);
        droppedEnrollment.drop();
        enrollments.save(droppedEnrollment);
        entityManager.flush();
        entityManager.clear();

        var page = classes.findEnrolledClassesByStudentIdAndStatus(
                99L, EnrollmentStatus.ENROLLED, PageRequest.of(0, 1));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(ClassEntity::getName).containsExactly("Enrolled");
        assertThat(classes.findByIdWithLock(enrolled.getId())).isPresent();
        assertThat(classes.findByIdWithLock(-1L)).isEmpty();
    }

    @Test
    void foreignAndMissingTenantCannotReadClassOrEnrollment() {
        ClassEntity ownClass = classes.save(ClassEntity.create("Tenant one", 7L, null, null));
        ClassEnrollment ownEnrollment = enrollments.save(ClassEnrollment.create(ownClass, 99L));
        entityManager.flush();
        entityManager.clear();

        jdbc.execute("SET LOCAL app.tenant_id = '2'");
        assertThat(classes.findById(ownClass.getId())).isEmpty();
        assertThat(enrollments.findById(ownEnrollment.getId())).isEmpty();
        assertThat(classes.findEnrolledClassesByStudentIdAndStatus(
                99L, EnrollmentStatus.ENROLLED, PageRequest.of(0, 10))).isEmpty();

        jdbc.execute("SET LOCAL app.tenant_id = ''");
        assertThat(classes.findById(ownClass.getId())).isEmpty();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = ClassEntity.class)
    @EnableJpaRepositories(basePackageClasses = ClassJpaRepository.class)
    @EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
    @Import({ClassPersistenceAdapter.class, ClassEnrollmentPersistenceAdapter.class})
    static class JpaTestConfiguration {
        @Bean
        DateTimeProvider auditingDateTimeProvider() {
            return () -> Optional.of(Instant.now());
        }
    }
}
