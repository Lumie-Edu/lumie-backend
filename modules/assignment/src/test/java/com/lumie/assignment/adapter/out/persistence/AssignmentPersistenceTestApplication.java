package com.lumie.assignment.adapter.out.persistence;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan("com.lumie.assignment.domain.entity")
@EnableJpaRepositories("com.lumie.assignment.adapter.out.persistence")
@EnableJpaAuditing(dateTimeProviderRef = "assignmentDateTimeProvider")
class AssignmentPersistenceTestApplication {
    @Bean
    DateTimeProvider assignmentDateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }

    static void enableTenantRls(JdbcTemplate jdbcTemplate, String table) {
        jdbcTemplate.execute("ALTER TABLE " + table + " ENABLE ROW LEVEL SECURITY");
        jdbcTemplate.execute("ALTER TABLE " + table + " FORCE ROW LEVEL SECURITY");
        jdbcTemplate.execute("CREATE POLICY tenant_isolation ON " + table + " "
                + "USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint) "
                + "WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)");
        jdbcTemplate.execute("SELECT set_config('app.tenant_id', '11', true)");
        Map<String, Object> role = jdbcTemplate.queryForMap("""
                SELECT current_user AS username, role.rolsuper AS superuser,
                       role.rolbypassrls AS bypass_rls, current_setting('app.tenant_id', true) AS tenant_id
                FROM pg_roles role WHERE role.rolname = current_user
                """);
        assertThat(role).containsEntry("username", "lumie_app")
                .containsEntry("superuser", false).containsEntry("bypass_rls", false)
                .containsEntry("tenant_id", "11");
    }
}
