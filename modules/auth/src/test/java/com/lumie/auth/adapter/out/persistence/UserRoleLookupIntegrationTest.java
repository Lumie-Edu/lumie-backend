package com.lumie.auth.adapter.out.persistence;

import com.lumie.auth.domain.entity.User;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ContextConfiguration(classes = UserRoleLookupIntegrationTest.JpaTestConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserPersistenceAdapter.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserRoleLookupIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired private UserJpaRepository repository;
    @Autowired private UserPersistenceAdapter adapter;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void batchRoleLookupRespectsTransactionLocalTenantRls() {
        TenantContextHolder.setTenant("first");
        TenantContextHolder.setTenantId(11L);
        Long studentId = repository.saveAndFlush(User.create("student-role-lookup", "Student", null,
                "hash", Role.STUDENT)).getId();
        TenantContextHolder.setTenant("second");
        TenantContextHolder.setTenantId(12L);
        Long ownerId = repository.saveAndFlush(User.create("owner-role-lookup", "Owner", null,
                "hash", Role.OWNER)).getId();
        entityManager.clear();

        jdbc.execute("CREATE ROLE role_lookup_reader");
        jdbc.execute("GRANT USAGE ON SCHEMA public TO role_lookup_reader");
        jdbc.execute("GRANT SELECT ON users TO role_lookup_reader");
        jdbc.execute("ALTER TABLE users ENABLE ROW LEVEL SECURITY");
        jdbc.execute("ALTER TABLE users FORCE ROW LEVEL SECURITY");
        jdbc.execute("CREATE POLICY tenant_isolation ON users USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)");
        jdbc.execute("SET LOCAL ROLE role_lookup_reader");

        jdbc.execute("SELECT set_config('app.tenant_id', '11', true)");
        assertThat(adapter.getUserRoles(List.of(studentId, ownerId)))
                .isEqualTo(Map.of(studentId, "STUDENT"));

        jdbc.execute("SELECT set_config('app.tenant_id', '12', true)");
        assertThat(adapter.getUserRoles(List.of(studentId, ownerId)))
                .isEqualTo(Map.of(ownerId, "OWNER"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = User.class)
    @EnableJpaRepositories(basePackageClasses = UserJpaRepository.class)
    @EnableJpaAuditing
    static class JpaTestConfiguration {}
}
