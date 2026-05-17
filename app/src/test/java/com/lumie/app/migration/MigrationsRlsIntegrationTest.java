package com.lumie.app.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration coverage for the two things the audit flagged as untested
 * and highest-risk (findings E.2 / E.8 / D.9 and E.7):
 *
 * <ol>
 *   <li><b>Migration safety</b> — every Flyway migration under
 *       {@code classpath:db/migration/public} applies top-to-bottom on a
 *       fresh PostgreSQL with no checksum/ordering breakage. This is the
 *       net that {@code flyway.repair()}-on-boot used to mask; with that
 *       gone, a broken migration must fail loudly here instead of in prod.</li>
 *   <li><b>Hostile-tenant isolation</b> — with the schema migrated, a
 *       connection as a {@code NOSUPERUSER NOBYPASSRLS} role (the prod
 *       {@code lumie_app} shape) cannot read another tenant's rows. This
 *       is the defining test for the RLS model: store as tenant 1,
 *       observe as tenant 2, assert nothing leaks.</li>
 * </ol>
 *
 * <p>The container's default user is a superuser and would silently
 * bypass RLS ({@code FORCE ROW LEVEL SECURITY} still does not bind
 * superusers / {@code BYPASSRLS}), so the isolation assertions run as a
 * dedicated restricted role created below — without that, this test would
 * pass even with isolation completely broken.
 */
@Testcontainers
class MigrationsRlsIntegrationTest {

    private static final String APP_ROLE = "lumie_app_test";
    private static final String APP_ROLE_PASSWORD = "lumie_app_test_pw";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @BeforeAll
    static void migrate() {
        // baselineOnMigrate/baselineVersion deliberately mirror the prod
        // Flyway config (application.yaml spring.flyway.baseline-*). The
        // point of this test is to reproduce exactly what runs on deploy,
        // not an idealized clean-slate run. On a fresh container (no
        // flyway_schema_history) baseline 0 < V1, so the whole chain still
        // applies — asserted explicitly below so a silent V1 skip fails.
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration/public")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();

        MigrateResult migrateResult;
        try {
            migrateResult = flyway.migrate();
        } catch (Exception e) {
            throw new AssertionError(
                    "Flyway migration chain failed to apply on fresh PostgreSQL", e);
        }
        assertThat(migrateResult.success)
                .as("all db/migration/public migrations applied successfully")
                .isTrue();
        assertThat(migrateResult.migrationsExecuted)
                .as("the migration chain is non-empty (RLS baseline + follow-ups)")
                .isGreaterThan(0);

        // Defang the baseline-skip risk: assert V1 actually ran. If the
        // baseline ever silently swallowed the first migration, the lowest
        // applied version would not be "1" and this fails loudly.
        var applied = flyway.info().applied();
        assertThat(applied).as("at least one migration applied").isNotEmpty();
        assertThat(applied[0].getVersion().toString())
                .as("the chain starts at V1 (nothing baselined away)")
                .isEqualTo("1");

        // validateWithResult() reports any applied-checksum / ordering drift
        // — the exact class of breakage the old auto-repair() silently hid.
        var validation = flyway.validateWithResult();
        assertThat(validation.validationSuccessful)
                .as("Flyway validate passes (no checksum/order drift): %s",
                        validation.getAllErrorMessages())
                .isTrue();

        createRestrictedAppRole();
    }

    /**
     * Mirrors the prod split: migrations run as the privileged migrator
     * (container superuser here); the runtime connects as a role that is
     * {@code NOSUPERUSER NOBYPASSRLS} so RLS policies actually bind.
     */
    private static void createRestrictedAppRole() {
        try (Connection admin = adminConnection();
             Statement st = admin.createStatement()) {
            st.execute("CREATE ROLE " + APP_ROLE
                    + " LOGIN PASSWORD '" + APP_ROLE_PASSWORD + "' NOSUPERUSER NOBYPASSRLS");
            // Mirror the prod lumie_app privilege set (HARD_RULES_CROSS:
            // SELECT,INSERT,UPDATE,DELETE) so the role is a faithful stand-in.
            st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON activity_logs TO " + APP_ROLE);
            st.execute("GRANT USAGE ON SEQUENCE activity_logs_id_seq TO " + APP_ROLE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to provision restricted app role", e);
        }
    }

    @Test
    void rlsHidesRowsFromOtherTenantsForRestrictedRole() throws Exception {
        try (Connection conn = appRoleConnection()) {
            conn.setAutoCommit(true);

            // Tenant 1 writes one activity_logs row under its own GUC.
            setTenant(conn, "1");
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(
                        "INSERT INTO activity_logs "
                        + "(id, actor_id, actor_name, actor_role, action, entity_type, tenant_id) "
                        + "VALUES (nextval('activity_logs_id_seq'), 1, 'tenant1-actor', "
                        + "'OWNER', 'CREATE', 'STUDENT', 1)");
            }
            assertThat(countActivityLogs(conn))
                    .as("tenant 1 sees its own row")
                    .isEqualTo(1);

            // Tenant 2 must not see tenant 1's row.
            setTenant(conn, "2");
            assertThat(countActivityLogs(conn))
                    .as("tenant 2 cannot read tenant 1 rows (RLS isolation)")
                    .isZero();

            // No tenant GUC at all → policy evaluates to false, zero rows.
            try (Statement st = conn.createStatement()) {
                st.execute("RESET app.tenant_id");
            }
            assertThat(countActivityLogs(conn))
                    .as("with no app.tenant_id set, all tenant-scoped rows are invisible")
                    .isZero();

            // Back to tenant 1 → row visible again (proves it was hidden,
            // not deleted).
            setTenant(conn, "1");
            assertThat(countActivityLogs(conn))
                    .as("tenant 1 still sees its row after the hostile reads")
                    .isEqualTo(1);
        }
    }

    private static void setTenant(Connection conn, String tenantId) throws Exception {
        try (Statement st = conn.createStatement()) {
            // SET is GUC/DDL-like; PreparedStatement parameters are not
            // accepted here. tenantId is a hardcoded test literal, not user
            // input — no injection surface.
            st.execute("SET app.tenant_id = '" + tenantId + "'");
        }
    }

    private static int countActivityLogs(Connection conn) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM activity_logs")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static Connection adminConnection() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static Connection appRoleConnection() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), APP_ROLE, APP_ROLE_PASSWORD);
    }
}
