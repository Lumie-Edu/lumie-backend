package com.lumie.app.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

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
 *   <li><b>Test-role shape guard</b> — the restricted test role must remain
 *       {@code NOSUPERUSER NOBYPASSRLS}; if the role were elevated the RLS
 *       isolation tests would trivially pass under a bypassing role and give
 *       a false green. See {@link #testRoleIsRestrictedSoRlsIsolationTestsAreValid()}.</li>
 *   <li><b>Admin-inserted cross-tenant isolation</b> — a row inserted by
 *       the privileged migrator session must not be visible to an app-role
 *       session operating under a different tenant's GUC context, even when
 *       the physical row is present in the table.</li>
 * </ol>
 *
 * <p>The container's default user is a superuser and would silently
 * bypass RLS ({@code FORCE ROW LEVEL SECURITY} still does not bind
 * superusers / {@code BYPASSRLS}), so the isolation assertions run as a
 * dedicated restricted role created below — without that, this test would
 * pass even with isolation completely broken.
 *
 * <p><b>Flyway lock note</b> — Flyway's default PostgreSQL lock uses
 * {@code pg_advisory_xact_lock}, which participates in transaction snapshots.
 * That transactional advisory lock causes {@code CREATE INDEX CONCURRENTLY}
 * (used in V21 and V23) to wait for Flyway's own open transaction, resulting
 * in a self-deadlock that hangs indefinitely. Setting
 * {@code flyway.postgresql.transactional.lock=false} switches to a session-level
 * advisory lock ({@code pg_advisory_lock}), which is invisible to
 * {@code CONCURRENTLY}'s lock-wait scan and eliminates the deadlock.
 *
 * <p><b>Prod-role pre-creation</b> — V22 issues a {@code GRANT} to the
 * {@code lumie_app} runtime role, and V27 issues
 * {@code ALTER DEFAULT PRIVILEGES FOR ROLE postgres ...}. In production those
 * role names already exist before Flyway runs. TestContainers starts a blank
 * cluster with only the container's default superuser, so {@link #migrate()}
 * pre-creates minimal placeholder roles for {@code lumie_app} and
 * {@code postgres} before calling {@code flyway.migrate()}.
 */
@Tag("integration")
@Testcontainers
@Timeout(value = 90, unit = TimeUnit.SECONDS)
class MigrationsRlsIntegrationTest {

    private static final String APP_ROLE = "lumie_app_test";
    private static final String APP_ROLE_PASSWORD = "lumie_app_test_pw";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @BeforeAll
    static void migrate() {
        // Pre-create the prod role names that migrations assume already exist.
        // V22 grants to lumie_app; V27 alters default privileges for role
        // postgres. In production the DBA provisions those roles before
        // Flyway runs; TestContainers provides a blank cluster with only the
        // container's default superuser, so we mirror that prerequisite here.
        createProdRole("lumie_app");
        createProdRole("postgres");

        // baselineOnMigrate/baselineVersion deliberately mirror the prod
        // Flyway config (application.yaml spring.flyway.baseline-*). The
        // point of this test is to reproduce exactly what runs on deploy,
        // not an idealized clean-slate run. On a fresh container (no
        // flyway_schema_history) baseline 0 < V1, so the whole chain still
        // applies — asserted explicitly below so a silent V1 skip fails.
        //
        // flyway.postgresql.transactional.lock=false: V21 and V23 use
        // CREATE INDEX CONCURRENTLY, which deadlocks with Flyway's default
        // pg_advisory_xact_lock. Session-level locking avoids this. Prod mirrors
        // this via spring.flyway.postgresql.transactional-lock=false in
        // application.yaml, so this run reproduces deploy exactly. See class Javadoc.
        var configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration/public")
                .baselineOnMigrate(true)
                .baselineVersion("0");
        configuration
                .getConfigurationExtension(PostgreSQLConfigurationExtension.class)
                .setTransactionalLock(false);
        Flyway flyway = configuration.load();

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
     * Creates a minimal placeholder role with the given name if it does not already exist.
     * Used to pre-provision production role names (e.g. {@code lumie_app}) that the
     * migration scripts assume exist — matching the prod DBA setup step that TestContainers
     * cannot replicate automatically.
     */
    private static void createProdRole(String roleName) {
        try (Connection admin = adminConnection();
             Statement st = admin.createStatement()) {
            st.execute(
                    "DO $$ "
                    + "BEGIN "
                    + "IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '" + roleName + "') THEN "
                    + "CREATE ROLE " + roleName + " NOSUPERUSER NOBYPASSRLS NOLOGIN; "
                    + "END IF; "
                    + "END $$");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to pre-create prod role: " + roleName, e);
        }
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
            st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON classes TO " + APP_ROLE);
            st.execute("GRANT USAGE ON SEQUENCE classes_id_seq TO " + APP_ROLE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to provision restricted app role", e);
        }
    }

    @Test
    void rlsHidesRowsFromOtherTenantsForRestrictedRole() throws Exception {
        try (Connection conn = appRoleConnection()) {
            conn.setAutoCommit(true);

            // Tenant 1 writes one classes row under its own GUC.
            setTenant(conn, "1");
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(
                        "INSERT INTO classes "
                        + "(id, name, tenant_id) "
                        + "VALUES (nextval('classes_id_seq'), 'tenant1-class', 1)");
            }
            assertThat(countClasses(conn))
                    .as("tenant 1 sees its own row")
                    .isEqualTo(1);

            // Tenant 2 must not see tenant 1's row.
            setTenant(conn, "2");
            assertThat(countClasses(conn))
                    .as("tenant 2 cannot read tenant 1 rows (RLS isolation)")
                    .isZero();

            // No tenant GUC at all → policy evaluates to false, zero rows.
            try (Statement st = conn.createStatement()) {
                st.execute("RESET app.tenant_id");
            }
            assertThat(countClasses(conn))
                    .as("with no app.tenant_id set, all tenant-scoped rows are invisible")
                    .isZero();

            // Back to tenant 1 → row visible again (proves it was hidden,
            // not deleted).
            setTenant(conn, "1");
            assertThat(countClasses(conn))
                    .as("tenant 1 still sees its row after the hostile reads")
                    .isEqualTo(1);
        }
    }

    /**
     * Verifies that the restricted test role ({@value APP_ROLE}) used by the RLS isolation
     * tests in this class is genuinely {@code NOSUPERUSER NOBYPASSRLS}.
     *
     * <p>This matters because a bypassing role would make the isolation tests trivially pass
     * even with RLS completely broken — the isolation tests are only meaningful when the
     * role they run as is actually bound by RLS policies. Asserting the role attributes here
     * ensures that invariant cannot silently regress via an accidental {@code ALTER ROLE}
     * during test-infrastructure changes.
     *
     * <p><b>Scope:</b> this test verifies the <em>test fixture</em> role {@value APP_ROLE},
     * not the production {@code lumie_app} role. TestContainers provisions its own blank
     * cluster with no prod roles present. Detecting drift in the real {@code lumie_app}
     * role requires a live-cluster check (e.g. querying the dev/prod Postgres directly)
     * and is out of scope for this TestContainers suite.
     */
    @Test
    void testRoleIsRestrictedSoRlsIsolationTestsAreValid() throws Exception {
        try (Connection admin = adminConnection();
             Statement st = admin.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT rolsuper, rolbypassrls FROM pg_roles WHERE rolname = '"
                     + APP_ROLE + "'")) {
            assertThat(rs.next())
                    .as("test fixture role '%s' must exist in pg_roles", APP_ROLE)
                    .isTrue();
            assertThat(rs.getBoolean("rolsuper"))
                    .as("test fixture role '%s' must NOT be a superuser "
                        + "(a superuser would bypass RLS, making isolation tests vacuous)", APP_ROLE)
                    .isFalse();
            assertThat(rs.getBoolean("rolbypassrls"))
                    .as("test fixture role '%s' must NOT have BYPASSRLS "
                        + "(BYPASSRLS would bypass RLS, making isolation tests vacuous)", APP_ROLE)
                    .isFalse();
        }
    }

    /**
     * Cross-tenant isolation via an admin-inserted row.
     *
     * <p>The privileged migrator connection (container superuser) inserts a row for tenant B.
     * The superuser bypasses {@code FORCE ROW LEVEL SECURITY} unconditionally — the
     * {@code tenant_id} literal in the {@code VALUES} clause is what the RLS {@code USING}
     * clause will filter on for restricted sessions.
     *
     * <p>An app-role session then operates under tenant A's context and asserts it
     * cannot see the row — even though it physically exists in the table. Switching the
     * same app-role session to tenant B's context proves the row is hidden, not absent.
     *
     * <p>Isolation is asserted by the specific inserted row's {@code id} (captured via
     * {@code currval}) rather than a broad {@code WHERE tenant_id = ...} count, so the
     * assertion is robust to rows inserted by other tests sharing the container.
     */
    @Test
    void adminInsertedRowForTenantBIsInvisibleToTenantASession() throws Exception {
        long tenantA = 100L;
        long tenantB = 200L;

        // Insert a row for tenant B using the privileged admin connection.
        // Superuser bypasses FORCE ROW LEVEL SECURITY; the tenant_id literal (tenantB)
        // is what the RLS USING clause filters on for non-superuser sessions.
        // setTenant is intentionally NOT called here: the GUC has no effect on a
        // superuser INSERT — the tenant_id value in VALUES is the sole source of truth.
        long insertedId;
        try (Connection admin = adminConnection()) {
            admin.setAutoCommit(true);
            try (Statement st = admin.createStatement()) {
                st.executeUpdate(
                        "INSERT INTO classes "
                        + "(id, name, tenant_id) "
                        + "VALUES (nextval('classes_id_seq'), 'tenant-b-class', " + tenantB + ")");
            }
            // Capture the exact id to make subsequent assertions row-specific,
            // not vulnerable to cross-test row contamination.
            try (Statement st = admin.createStatement();
                 ResultSet rs = st.executeQuery("SELECT currval('classes_id_seq')")) {
                rs.next();
                insertedId = rs.getLong(1);
            }
        }

        // App-role session under tenant A must not see the tenant B row.
        try (Connection appConn = appRoleConnection()) {
            appConn.setAutoCommit(true);
            setTenant(appConn, String.valueOf(tenantA));

            try (Statement st = appConn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT count(*) FROM classes WHERE id = " + insertedId)) {
                rs.next();
                assertThat(rs.getInt(1))
                        .as("app-role session for tenant %d must not see row id=%d "
                            + "belonging to tenant %d (RLS cross-tenant isolation)",
                            tenantA, insertedId, tenantB)
                        .isZero();
            }

            // Switch to tenant B — the specific row must now be visible (proves hidden,
            // not deleted).
            setTenant(appConn, String.valueOf(tenantB));
            try (Statement st = appConn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT count(*) FROM classes WHERE id = " + insertedId)) {
                rs.next();
                assertThat(rs.getInt(1))
                        .as("app-role session switched to tenant %d must now see row id=%d "
                            + "(proves the row was hidden by RLS, not deleted)",
                            tenantB, insertedId)
                        .isEqualTo(1);
            }
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

    private static int countClasses(Connection conn) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM classes")) {
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
