package com.lumie.staff.adapter.out.persistence;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.staff.application.port.out.PermissionPersistencePort;
import com.lumie.staff.application.port.out.StaffPermissionPersistencePort;
import com.lumie.staff.application.port.out.StaffPersistencePort;
import com.lumie.staff.domain.entity.Permission;
import com.lumie.staff.domain.entity.Staff;
import com.lumie.staff.domain.entity.StaffPermission;
import com.lumie.staff.domain.vo.AccessLevel;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.List;
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
@ContextConfiguration(classes = StaffPersistenceJpaIntegrationTest.JpaTestConfiguration.class)
class StaffPersistenceJpaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private final StaffPersistencePort staff;
    private final PermissionPersistencePort permissions;
    private final StaffPermissionPersistencePort staffPermissions;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;

    StaffPersistenceJpaIntegrationTest(StaffPersistencePort staff,
                                       PermissionPersistencePort permissions,
                                       StaffPermissionPersistencePort staffPermissions,
                                       EntityManager entityManager,
                                       JdbcTemplate jdbc) {
        this.staff = staff;
        this.permissions = permissions;
        this.staffPermissions = staffPermissions;
        this.entityManager = entityManager;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void bindRestrictedTenant() {
        TenantContextHolder.setTenant("staff-it");
        TenantContextHolder.setTenantId(1L);
        jdbc.execute("""
                CREATE TABLE users (
                    id bigint primary key,
                    name varchar(100),
                    phone varchar(20),
                    email varchar(255),
                    role varchar(20))
                """);
        jdbc.update("""
                INSERT INTO users (id, name, phone, email, role)
                VALUES (901, 'Teacher', '01087654321', 'teacher@example.com', 'INSTRUCTOR'),
                       (902, 'Second', '01087654322', 'second@example.com', 'INSTRUCTOR')
                """);
        for (String table : new String[]{"staff", "permissions", "staff_permissions"}) {
            jdbc.execute("ALTER TABLE " + table + " ENABLE ROW LEVEL SECURITY");
            jdbc.execute("ALTER TABLE " + table + " FORCE ROW LEVEL SECURITY");
            jdbc.execute("CREATE POLICY tenant_isolation ON " + table
                    + " USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)"
                    + " WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)");
        }
        jdbc.execute("CREATE ROLE staff_rls_test NOSUPERUSER NOBYPASSRLS");
        jdbc.execute("GRANT USAGE ON SCHEMA public TO staff_rls_test");
        jdbc.execute("GRANT SELECT ON users TO staff_rls_test");
        jdbc.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON staff, permissions, staff_permissions TO staff_rls_test");
        jdbc.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO staff_rls_test");
        jdbc.execute("SET LOCAL ROLE staff_rls_test");
        jdbc.execute("SET LOCAL app.tenant_id = '1'");
        assertThat(jdbc.queryForObject("SELECT current_user", String.class)).isEqualTo("staff_rls_test");
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("ATDD-US1-001:example-012 staff profile lookup")
    void profileLookupReadsCanonicalIdentity() {
        Staff saved = staff.save(Staff.create(901L, "staff_901", "Teacher",
                "01087654321", "teacher@example.com", null));
        entityManager.flush();
        entityManager.clear();

        Staff loaded = staff.findByUserId(901L).orElseThrow();
        assertThat(loaded.getStaffName()).isEqualTo("Teacher");
        assertThat(loaded.getStaffPhone()).isEqualTo("01087654321");
        assertThat(staff.search(true, "teacher@example.com", PageRequest.of(0, 10)))
                .hasSize(1);
        assertThat(saved.getId()).isNotNull();
    }

    @Test
    @DisplayName("ATDD-US1-001:example-011 staff permission lookup")
    void permissionLookupPreservesCodeAndAccessLevel() {
        Staff saved = staff.save(Staff.create(901L, "staff_901", "Teacher",
                "01087654321", "teacher@example.com", null));
        Permission permission = Permission.create("REPORT_VIEW", "Report view", "reports", null);
        entityManager.persist(permission);
        staffPermissions.saveAll(List.of(StaffPermission.of(saved.getId(), "REPORT_VIEW", AccessLevel.READ)));
        entityManager.flush();
        entityManager.clear();

        assertThat(permissions.findByCategory("reports"))
                .extracting(Permission::getCode).containsExactly("REPORT_VIEW");
        assertThat(staffPermissions.findByStaffId(saved.getId()))
                .extracting(StaffPermission::getAccessLevel).containsExactly(AccessLevel.READ);
    }

    @Test
    void searchPreservesActiveFilterAndCanonicalSortMappings() {
        staff.save(Staff.create(901L, "staff_901", "Teacher",
                "01087654321", "teacher@example.com", null));
        Staff inactive = Staff.create(902L, "staff_902", "Second",
                "01087654322", "second@example.com", null);
        inactive.deactivate();
        staff.save(inactive);
        entityManager.flush();
        entityManager.clear();

        assertThat(staff.search(null, null, Pageable.unpaged()).getContent()).hasSize(2);
        assertThat(staff.search(false, " ", PageRequest.of(0, 10, Sort.by("name"))).getContent())
                .extracting(Staff::getStaffName).containsExactly("Second");
        assertThat(staff.search(true, "TEACHER", PageRequest.of(0, 10, Sort.by("createdAt"))).getContent())
                .extracting(Staff::getStaffName).containsExactly("Teacher");
    }

    @Test
    void foreignAndMissingTenantCannotReadStaffPermissions() {
        Staff saved = staff.save(Staff.create(901L, "staff_901", "Teacher",
                "01087654321", "teacher@example.com", null));
        entityManager.persist(Permission.create("REPORT_VIEW", "Report view", "reports", null));
        staffPermissions.saveAll(List.of(StaffPermission.of(saved.getId(), "REPORT_VIEW", AccessLevel.READ)));
        entityManager.flush();
        entityManager.clear();

        jdbc.execute("SET LOCAL app.tenant_id = '2'");
        assertThat(staff.findById(saved.getId())).isEmpty();
        assertThat(permissions.findByCategory("reports")).isEmpty();
        assertThat(staffPermissions.findByStaffId(saved.getId())).isEmpty();

        jdbc.execute("SET LOCAL app.tenant_id = ''");
        assertThat(staff.findById(saved.getId())).isEmpty();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = Staff.class)
    @EnableJpaRepositories(basePackageClasses = StaffJpaRepository.class)
    @EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
    @Import({StaffPersistenceAdapter.class, PermissionPersistenceAdapter.class,
            StaffPermissionPersistenceAdapter.class})
    static class JpaTestConfiguration {
        @Bean
        DateTimeProvider auditingDateTimeProvider() {
            return () -> Optional.of(Instant.now());
        }
    }
}
