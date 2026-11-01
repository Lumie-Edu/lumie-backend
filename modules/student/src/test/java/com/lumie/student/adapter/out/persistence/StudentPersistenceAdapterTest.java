package com.lumie.student.adapter.out.persistence;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.student.application.port.out.StudentPersistencePort;
import com.lumie.student.domain.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@DataJpaTest
@ContextConfiguration(classes = StudentPersistenceAdapterTest.JpaTestConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(StudentPersistenceAdapter.class)
class StudentPersistenceAdapterTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired StudentPersistencePort students;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenant("student-test");
        TenantContextHolder.setTenantId(1L);
        jdbc.execute("CREATE TABLE IF NOT EXISTS users (id BIGINT PRIMARY KEY, name VARCHAR(255), phone VARCHAR(32))");
        jdbc.update("INSERT INTO users (id, name, phone) VALUES (10, 'Alpha', '01011111111'), (11, 'Beta', '01022222222') ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, phone = EXCLUDED.phone");
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("ATDD-US1-001:example-013 student listing")
    void preservesCrudDerivedSearchAndStatisticsOperations() {
        Student alpha = students.save(Student.create(10L, "alpha-login", "Alpha", "01011111111", "Alpha High", 2008, null, null));
        Student beta = Student.create(11L, "beta-login", "Beta", "01022222222", "Beta High", 2009, null, null);
        beta.deactivate();
        beta = students.save(beta);

        assertThat(students.findById(alpha.getId())).isPresent();
        assertThat(students.findAllById(List.of(alpha.getId(), beta.getId()))).hasSize(2);
        assertThat(students.findByUserId(10L)).isPresent();
        assertThat(students.findByUserIdIn(List.of(10L, 11L))).hasSize(2);
        assertThat(students.findByUserLoginId("alpha-login")).isPresent();
        assertThat(students.findByCanonicalPhone("01011111111")).isPresent();
        assertThat(students.findByCanonicalPhone("missing")).isEmpty();
        assertThat(students.existsByUserLoginId("alpha-login")).isTrue();
        assertThat(students.existsByCanonicalPhone("01011111111")).isTrue();
        assertThat(students.existsByCanonicalPhoneExcluding("01011111111", beta.getId())).isTrue();
        assertThat(students.existsByCanonicalPhoneExcluding("01011111111", alpha.getId())).isFalse();
        assertThat(students.findByIsActive(true, PageRequest.of(0, 10)).getContent()).containsExactly(alpha);
        assertThat(students.countByIsActive(false)).isEqualTo(1);
        assertThat(students.findAll(PageRequest.of(0, 10, Sort.by("id"))).getContent()).hasSize(2);
        assertThat(students.search(true, "alpha", null, null, PageRequest.of(0, 10, Sort.by("name"))).getContent())
                .extracting(Student::getUserId).containsExactly(10L);
        assertThat(students.search(null, null, List.of(alpha.getId()), List.of(beta.getId()), PageRequest.of(0, 10)).getContent())
                .extracting(Student::getUserId).containsExactly(10L);

        Instant now = Instant.now().plusSeconds(1);
        assertThat(students.countByCreatedAtBetween(Instant.EPOCH, now)).isEqualTo(2);
        assertThat(students.countByCreatedAtBefore(now)).isEqualTo(2);
        assertThat(students.countByIsActiveAndUpdatedAtBetween(false, Instant.EPOCH, now)).isEqualTo(1);
        assertThat(students.countByIsActiveAndUpdatedAtBefore(false, now)).isEqualTo(1);
        assertThat(students.countMonthlyEnrollmentsSince(Instant.EPOCH)).hasSize(1);
        assertThat(students.countMonthlyDropoutsSince(Instant.EPOCH)).hasSize(1);

        Student added = Student.create(12L, "third", "Third", "01033333333", null, null, null, null);
        jdbc.update("INSERT INTO users (id, name, phone) VALUES (12, 'Third', '01033333333')");
        assertThat(students.saveAll(List.of(added))).hasSize(1);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Student.class)
    @EnableJpaRepositories(basePackageClasses = StudentJpaRepository.class)
    @EnableJpaAuditing
    static class JpaTestConfiguration {}
}
