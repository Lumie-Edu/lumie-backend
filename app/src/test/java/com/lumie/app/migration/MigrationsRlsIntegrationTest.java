package com.lumie.app.migration;

import com.lumie.auth.domain.entity.User;
import com.lumie.common.auth.Role;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
            // Mirror the prod lumie_app privilege set (workspace AGENTS.md:
            // SELECT,INSERT,UPDATE,DELETE) so the role is a faithful stand-in.
            st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON classes TO " + APP_ROLE);
            st.execute("GRANT USAGE ON SEQUENCE classes_id_seq TO " + APP_ROLE);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to provision restricted app role", e);
        }
    }

    @Test
    void readOnlyChatbotRetiresConfirmedActionSchema() throws Exception {
        try (Connection admin = adminConnection();
             Statement st = admin.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT to_regclass('public.ai_confirmed_actions') IS NULL AS actions_removed,
                            to_regclass('public.idx_ai_chat_messages_id_tenant_id') IS NULL AS action_index_removed,
                            to_regclass('public.idx_sms_messages_tenant_source_action') IS NULL AS sms_index_removed,
                            NOT EXISTS (
                                SELECT 1 FROM information_schema.columns
                                 WHERE table_schema = 'public' AND table_name = 'sms_messages'
                                   AND column_name = 'source_action_id'
                            ) AS sms_column_removed,
                            (SELECT count(*) FROM flyway_schema_history
                              WHERE version IN ('132', '133', '134') AND success) AS migration_count
                     """)) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getBoolean("actions_removed")).isTrue();
            assertThat(rs.getBoolean("action_index_removed")).isTrue();
            assertThat(rs.getBoolean("sms_index_removed")).isTrue();
            assertThat(rs.getBoolean("sms_column_removed")).isTrue();
            assertThat(rs.getInt("migration_count")).isEqualTo(3);
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

    @Test
    void userIdentityContractRemovesLegacyColumnsAndKeepsPhoneUniquenessOnUsers() throws Exception {
        try (Connection admin = adminConnection();
             Statement st = admin.createStatement();
             ResultSet columns = st.executeQuery("""
                     SELECT table_name, column_name, is_nullable
                     FROM information_schema.columns
                     WHERE table_schema = 'public'
                       AND table_name IN ('students', 'staff')
                       AND column_name IN ('name', 'phone', 'email')
                     """)) {
            var nullability = new java.util.HashMap<String, String>();
            while (columns.next()) {
                nullability.put(columns.getString("table_name") + "." + columns.getString("column_name"),
                        columns.getString("is_nullable"));
            }
            assertThat(nullability).isEmpty();
        }

        try (Connection admin = adminConnection();
             Statement st = admin.createStatement();
             ResultSet indexes = st.executeQuery("""
                     SELECT indexname
                     FROM pg_indexes
                     WHERE schemaname = 'public'
                       AND indexname IN ('idx_students_phone', 'idx_staff_phone', 'idx_staff_email',
                                         'uq_users_student_phone', 'uq_users_staff_phone')
                     """)) {
            var names = new java.util.HashSet<String>();
            while (indexes.next()) {
                names.add(indexes.getString("indexname"));
            }
            assertThat(names).containsExactlyInAnyOrder(
                    "uq_users_student_phone", "uq_users_staff_phone");
        }
    }

    @Test
    void userPhoneUniquenessPreservesSeparateStudentAndStaffGroups() throws Exception {
        try (Connection admin = adminConnection();
             Statement st = admin.createStatement()) {
            admin.setAutoCommit(false);
            st.executeUpdate("""
                    INSERT INTO users (user_login_id, password_hash, name, phone, role, tenant_id)
                    VALUES ('identity-student-a', 'test', 'Student A', '01000000001', 'STUDENT', 1)
                    """);
            st.executeUpdate("""
                    INSERT INTO users (user_login_id, password_hash, name, phone, role, tenant_id)
                    VALUES ('identity-staff-a', 'test', 'Staff A', '01000000001', 'INSTRUCTOR', 1)
                    """);

            var studentSavepoint = admin.setSavepoint();
            assertThatThrownBy(() -> st.executeUpdate("""
                    INSERT INTO users (user_login_id, password_hash, name, phone, role, tenant_id)
                    VALUES ('identity-student-b', 'test', 'Student B', '01000000001', 'STUDENT', 2)
                    """))
                    .isInstanceOf(SQLException.class)
                    .satisfies(error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23505"));
            admin.rollback(studentSavepoint);

            var staffSavepoint = admin.setSavepoint();
            assertThatThrownBy(() -> st.executeUpdate("""
                    INSERT INTO users (user_login_id, password_hash, name, phone, role, tenant_id)
                    VALUES ('identity-staff-b', 'test', 'Staff B', '01000000001', 'MANAGER', 2)
                    """))
                    .isInstanceOf(SQLException.class)
                    .satisfies(error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("23505"));
            admin.rollback(staffSavepoint);
            admin.rollback();
        }
    }

    @Test
    void usersWithoutPhonesCanBeCreatedRepeatedlyUnderPhoneUniquenessIndexes() throws Exception {
        try (Connection admin = adminConnection()) {
            admin.setAutoCommit(false);
            try (PreparedStatement insert = admin.prepareStatement("""
                    INSERT INTO users (user_login_id, password_hash, name, phone, role, tenant_id)
                    VALUES (?, 'test', ?, ?, ?, 1)
                    """)) {
                for (int i = 0; i < 4; i++) {
                    Role role = i < 2 ? Role.STUDENT : Role.INSTRUCTOR;
                    User user = User.create("identity-no-phone-" + i, "No Phone " + i,
                            i % 2 == 0 ? "" : "   ", "test", role);
                    assertThat(user.getPhone()).isNull();
                    insert.setString(1, user.getUserLoginId());
                    insert.setString(2, user.getName());
                    insert.setString(3, user.getPhone());
                    insert.setString(4, user.getRole().name());
                    insert.executeUpdate();
                }
            } finally {
                admin.rollback();
            }
        }
    }

    @Test
    void erd14HistoryConstraintsAreValidatedAndRetainCascadeDeletion() throws Exception {
        try (Connection admin = adminConnection();
             Statement st = admin.createStatement();
             ResultSet constraints = st.executeQuery("""
                     SELECT conname, convalidated, pg_get_constraintdef(oid) AS definition
                     FROM pg_constraint
                     WHERE conrelid IN ('question_results'::regclass,
                                        'ai_conversations'::regclass,
                                        'ai_chat_messages'::regclass)
                       AND contype = 'f'
                     """)) {
            var definitions = new java.util.HashMap<String, String>();
            while (constraints.next()) {
                assertThat(constraints.getBoolean("convalidated"))
                        .as("validated FK: %s", constraints.getString("conname"))
                        .isTrue();
                definitions.put(constraints.getString("conname"),
                        constraints.getString("definition"));
            }

            assertThat(definitions).containsKeys(
                    "question_results_exam_result_tenant_fkey",
                    "ai_conversations_user_tenant_fkey",
                    "ai_chat_messages_conversation_tenant_fkey");
            assertThat(definitions).doesNotContainKeys(
                    "question_results_exam_result_id_fkey",
                    "ai_chat_messages_conversation_id_fkey");
            assertThat(definitions.get("question_results_exam_result_tenant_fkey"))
                    .contains("FOREIGN KEY (exam_result_id, tenant_id)")
                    .contains("REFERENCES exam_results(id, tenant_id) ON DELETE CASCADE");
            assertThat(definitions.get("ai_conversations_user_tenant_fkey"))
                    .contains("FOREIGN KEY (user_id, tenant_id)")
                    .contains("REFERENCES users(id, tenant_id) ON DELETE CASCADE");
            assertThat(definitions.get("ai_chat_messages_conversation_tenant_fkey"))
                    .contains("FOREIGN KEY (conversation_id, tenant_id)")
                    .contains("REFERENCES ai_conversations(id, tenant_id) ON DELETE CASCADE");
        }
    }

    @Test
    void historicalConstraintsAreValidatedAndOnlyCompositeJobExamLinksRemain() throws Exception {
        try (Connection admin = adminConnection();
             Statement st = admin.createStatement();
             ResultSet constraints = st.executeQuery("""
                     SELECT conname, convalidated, pg_get_constraintdef(oid) AS definition
                     FROM pg_constraint
                     WHERE conname IN (
                         'exam_results_current_submission_fkey',
                         'payment_transactions_type_owner_check',
                         'omr_grading_jobs_exam_id_fkey',
                         'omr_grading_jobs_exam_tenant_fkey',
                         'report_generation_jobs_exam_id_fkey',
                         'report_generation_jobs_exam_tenant_fkey'
                     )
                     """)) {
            var definitions = new java.util.HashMap<String, String>();
            while (constraints.next()) {
                assertThat(constraints.getBoolean("convalidated"))
                        .as("validated constraint: %s", constraints.getString("conname"))
                        .isTrue();
                definitions.put(constraints.getString("conname"),
                        constraints.getString("definition"));
            }

            assertThat(definitions).containsKeys(
                    "exam_results_current_submission_fkey",
                    "payment_transactions_type_owner_check",
                    "omr_grading_jobs_exam_tenant_fkey",
                    "report_generation_jobs_exam_tenant_fkey");
            assertThat(definitions).doesNotContainKeys(
                    "omr_grading_jobs_exam_id_fkey",
                    "report_generation_jobs_exam_id_fkey");
            assertThat(definitions.get("exam_results_current_submission_fkey"))
                    .contains("FOREIGN KEY (current_submission_id, tenant_id)")
                    .contains("ON DELETE SET NULL (current_submission_id)");
            assertThat(definitions.get("omr_grading_jobs_exam_tenant_fkey"))
                    .contains("FOREIGN KEY (exam_id, tenant_id)")
                    .contains("ON DELETE RESTRICT");
            assertThat(definitions.get("report_generation_jobs_exam_tenant_fkey"))
                    .contains("FOREIGN KEY (exam_id, tenant_id)")
                    .contains("ON DELETE CASCADE");
        }
    }

    @Test
    void compositeJobExamLinksRejectCrossTenantWritesAndRetainDeletePolicies() throws Exception {
        long tenantA = 910_130L;
        long tenantB = 910_131L;
        long omrExamId = 930_130L;
        long reportExamId = 930_131L;

        try (Connection admin = adminConnection();
             Statement st = admin.createStatement()) {
            admin.setAutoCommit(false);
            try {
                insertExam(st, omrExamId, tenantA, "erd130-omr-exam");
                insertExam(st, reportExamId, tenantA, "erd130-report-exam");

                var wrongTenant = admin.setSavepoint();
                assertThatThrownBy(() -> st.executeUpdate(
                        "INSERT INTO omr_grading_jobs "
                        + "(exam_id, tenant_slug, total_images, tenant_id) VALUES ("
                        + omrExamId + ", 'erd130', 1, " + tenantB + ")"))
                        .isInstanceOf(SQLException.class)
                        .satisfies(error -> assertThat(((SQLException) error).getSQLState())
                                .isEqualTo("23503"));
                admin.rollback(wrongTenant);

                long omrJobId = insertReturningId(st,
                        "INSERT INTO omr_grading_jobs "
                        + "(exam_id, tenant_slug, total_images, tenant_id) VALUES ("
                        + omrExamId + ", 'erd130', 1, " + tenantA + ") RETURNING id");
                var restrictedDelete = admin.setSavepoint();
                assertThatThrownBy(() -> st.executeUpdate(
                        "DELETE FROM exams WHERE id = " + omrExamId))
                        .isInstanceOf(SQLException.class)
                        .satisfies(error -> assertThat(((SQLException) error).getSQLState())
                                .isEqualTo("23001"));
                admin.rollback(restrictedDelete);
                assertThat(countById(st, "omr_grading_jobs", omrJobId)).isEqualTo(1);

                long reportJobId = insertReturningId(st,
                        "INSERT INTO report_generation_jobs "
                        + "(exam_id, tenant_slug, tenant_id) VALUES ("
                        + reportExamId + ", 'erd130', " + tenantA + ") RETURNING id");
                st.executeUpdate("DELETE FROM exams WHERE id = " + reportExamId);
                assertThat(countById(st, "report_generation_jobs", reportJobId)).isZero();
            } finally {
                admin.rollback();
            }
        }
    }

    @Test
    void erd14RejectsCrossTenantLinksAndCascadesOwnedHistory() throws Exception {
        long tenantA = 910_101L;
        long tenantB = 910_102L;
        long examId = 930_101L;

        try (Connection admin = adminConnection();
             Statement st = admin.createStatement()) {
            admin.setAutoCommit(false);
            try {
                insertExam(st, examId, tenantA, "erd14-exam");
                long resultId = insertReturningId(st,
                        "INSERT INTO exam_results (exam_id, total_score, tenant_id) "
                        + "VALUES (" + examId + ", 0, " + tenantA + ") RETURNING id");

                var wrongQuestion = admin.setSavepoint();
                assertThatThrownBy(() -> st.executeUpdate(
                        "INSERT INTO question_results "
                        + "(exam_result_id, question_number, is_correct, score, tenant_id) "
                        + "VALUES (" + resultId + ", 1, false, 0, " + tenantB + ")"))
                        .isInstanceOf(SQLException.class)
                        .satisfies(error -> assertThat(((SQLException) error).getSQLState())
                                .isEqualTo("23503"));
                admin.rollback(wrongQuestion);

                long questionId = insertReturningId(st,
                        "INSERT INTO question_results "
                        + "(exam_result_id, question_number, is_correct, score, tenant_id) "
                        + "VALUES (" + resultId + ", 1, false, 0, " + tenantA + ") "
                        + "RETURNING id");
                st.executeUpdate("DELETE FROM exam_results WHERE id = " + resultId);
                assertThat(countById(st, "question_results", questionId)).isZero();

                long userId = insertReturningId(st,
                        "INSERT INTO users "
                        + "(user_login_id, password_hash, name, role, tenant_id) "
                        + "VALUES ('erd14-fk-owner', 'test', 'Owner', 'INSTRUCTOR', "
                        + tenantA + ") RETURNING id");

                var wrongConversation = admin.setSavepoint();
                assertThatThrownBy(() -> st.executeUpdate(
                        "INSERT INTO ai_conversations (user_id, tenant_id) "
                        + "VALUES (" + userId + ", " + tenantB + ")"))
                        .isInstanceOf(SQLException.class)
                        .satisfies(error -> assertThat(((SQLException) error).getSQLState())
                                .isEqualTo("23503"));
                admin.rollback(wrongConversation);

                long conversationId = insertReturningId(st,
                        "INSERT INTO ai_conversations (user_id, tenant_id) "
                        + "VALUES (" + userId + ", " + tenantA + ") RETURNING id");

                var wrongMessage = admin.setSavepoint();
                assertThatThrownBy(() -> st.executeUpdate(
                        "INSERT INTO ai_chat_messages "
                        + "(conversation_id, role, content, tenant_id) "
                        + "VALUES (" + conversationId + ", 'USER', 'test', "
                        + tenantB + ")"))
                        .isInstanceOf(SQLException.class)
                        .satisfies(error -> assertThat(((SQLException) error).getSQLState())
                                .isEqualTo("23503"));
                admin.rollback(wrongMessage);

                long messageId = insertReturningId(st,
                        "INSERT INTO ai_chat_messages "
                        + "(conversation_id, role, content, tenant_id) "
                        + "VALUES (" + conversationId + ", 'USER', 'test', "
                        + tenantA + ") RETURNING id");
                st.executeUpdate("DELETE FROM users WHERE id = " + userId);
                assertThat(countById(st, "ai_conversations", conversationId)).isZero();
                assertThat(countById(st, "ai_chat_messages", messageId)).isZero();
            } finally {
                admin.rollback();
            }
        }
    }

    @Test
    void remainingTenantLinksUseValidatedCompositeKeysAndPreserveDeletionRules() throws Exception {
        record Link(String child, String reference, String parent, String deleteAction) {}

        var links = java.util.List.of(
                new Link("announcements", "author_id", "users", "n"),
                new Link("assignment_submissions", "assignment_id", "assignments", "c"),
                new Link("assignment_submissions", "student_id", "students", "n"),
                new Link("class_enrollments", "class_id", "classes", "c"),
                new Link("class_enrollments", "student_id", "students", "n"),
                new Link("classes", "teacher_id", "staff", "n"),
                new Link("exam_results", "exam_id", "exams", "c"),
                new Link("exam_results", "omr_grading_job_id", "omr_grading_jobs", "n"),
                new Link("exam_results", "student_id", "students", "n"),
                new Link("file_metadata", "folder_id", "textbook_folders", "r"),
                new Link("invoices", "subscription_id", "subscriptions", "r"),
                new Link("lectures", "teacher_id", "staff", "n"),
                new Link("qna_comments", "qna_id", "qna_boards", "c"),
                new Link("sms_messages", "template_id", "sms_templates", "n"),
                new Link("sms_messages", "sender_id", "staff", "n"),
                new Link("staff", "user_id", "users", "c"),
                new Link("staff_permissions", "staff_id", "staff", "c"),
                new Link("students", "user_id", "users", "c"),
                new Link("subscriptions", "billing_key_id", "billing_keys", "n"),
                new Link("tax_invoices", "invoice_id", "invoices", "r"),
                new Link("textbook_folders", "parent_id", "textbook_folders", "r")
        );

        try (Connection admin = adminConnection();
             PreparedStatement constraints = admin.prepareStatement("""
                     SELECT convalidated, confdeltype, pg_get_constraintdef(oid) AS definition
                     FROM pg_constraint
                     WHERE contype = 'f'
                       AND conrelid = ?::regclass
                       AND confrelid = ?::regclass
                     """)) {
            for (Link link : links) {
                constraints.setString(1, link.child());
                constraints.setString(2, link.parent());
                boolean found = false;
                try (ResultSet rows = constraints.executeQuery()) {
                    String expected = "FOREIGN KEY (" + link.reference() + ", tenant_id) "
                            + "REFERENCES " + link.parent() + "(id, tenant_id)";
                    while (rows.next()) {
                        String definition = rows.getString("definition");
                        if (!definition.startsWith(expected)) {
                            continue;
                        }
                        found = true;
                        assertThat(rows.getBoolean("convalidated"))
                                .as("validated same-tenant link: %s.%s", link.child(), link.reference())
                                .isTrue();
                        assertThat(rows.getString("confdeltype"))
                                .as("unchanged delete action: %s.%s", link.child(), link.reference())
                                .isEqualTo(link.deleteAction());
                        if ("n".equals(link.deleteAction())) {
                            assertThat(definition)
                                    .as("tenant_id must survive parent deletion: %s.%s",
                                            link.child(), link.reference())
                                    .contains("ON DELETE SET NULL (" + link.reference() + ")");
                        }
                    }
                }
                assertThat(found)
                        .as("same-tenant link exists: %s.%s -> %s",
                                link.child(), link.reference(), link.parent())
                        .isTrue();
            }
        }
    }

    @Test
    void tenantLinkConstraintsRejectCrossTenantWritesAndKeepHistoryDeletionBehavior() throws Exception {
        long tenantA = 910_127L;
        long tenantB = 910_128L;
        String parentFolder = java.util.UUID.randomUUID().toString();
        String childFolder = java.util.UUID.randomUUID().toString();

        try (Connection admin = adminConnection();
             Statement st = admin.createStatement()) {
            admin.setAutoCommit(false);
            try {
                long authorId = insertReturningId(st,
                        "INSERT INTO users (user_login_id, password_hash, name, role, tenant_id) "
                        + "VALUES ('erd-link-author', 'test', 'Author', 'INSTRUCTOR', "
                        + tenantA + ") RETURNING id");

                var wrongAuthor = admin.setSavepoint();
                assertThatThrownBy(() -> st.executeUpdate(
                        "INSERT INTO announcements "
                        + "(announcement_title, announcement_content, audience_scope, author_id, tenant_id) "
                        + "VALUES ('Wrong tenant', 'test', 'ALL', " + authorId + ", " + tenantB + ")"))
                        .isInstanceOf(SQLException.class)
                        .satisfies(error -> assertThat(((SQLException) error).getSQLState())
                                .isEqualTo("23503"));
                admin.rollback(wrongAuthor);

                long noticeId = insertReturningId(st,
                        "INSERT INTO announcements "
                        + "(announcement_title, announcement_content, audience_scope, author_id, tenant_id) "
                        + "VALUES ('Same tenant', 'test', 'ALL', " + authorId + ", " + tenantA
                        + ") RETURNING id");
                st.executeUpdate("DELETE FROM users WHERE id = " + authorId);
                try (ResultSet notice = st.executeQuery(
                        "SELECT author_id, tenant_id FROM announcements WHERE id = " + noticeId)) {
                    assertThat(notice.next()).isTrue();
                    assertThat(notice.getObject("author_id")).isNull();
                    assertThat(notice.getLong("tenant_id")).isEqualTo(tenantA);
                }

                st.executeUpdate("INSERT INTO textbook_folders (id, name, tenant_id) "
                        + "VALUES ('" + parentFolder + "', 'Parent', " + tenantA + ")");
                var wrongFolder = admin.setSavepoint();
                assertThatThrownBy(() -> st.executeUpdate(
                        "INSERT INTO textbook_folders (id, name, parent_id, tenant_id) "
                        + "VALUES ('" + childFolder + "', 'Wrong tenant', '"
                        + parentFolder + "', " + tenantB + ")"))
                        .isInstanceOf(SQLException.class)
                        .satisfies(error -> assertThat(((SQLException) error).getSQLState())
                                .isEqualTo("23503"));
                admin.rollback(wrongFolder);

                st.executeUpdate("INSERT INTO textbook_folders (id, name, parent_id, tenant_id) "
                        + "VALUES ('" + childFolder + "', 'Child', '"
                        + parentFolder + "', " + tenantA + ")");
                var restrictedParent = admin.setSavepoint();
                assertThatThrownBy(() -> st.executeUpdate(
                        "DELETE FROM textbook_folders WHERE id = '" + parentFolder + "'"))
                        .isInstanceOf(SQLException.class)
                        .satisfies(error -> assertThat(((SQLException) error).getSQLState())
                                .isEqualTo("23001"));
                admin.rollback(restrictedParent);

                long classId = 930_127L;
                insertClass(st, classId, tenantA, "History class");
                long enrollmentId = insertReturningId(st,
                        "INSERT INTO class_enrollments (class_id, student_id, tenant_id) "
                        + "VALUES (" + classId + ", null, " + tenantA + ") RETURNING id");
                st.executeUpdate("DELETE FROM classes WHERE id = " + classId);
                assertThat(countById(st, "class_enrollments", enrollmentId)).isZero();
            } finally {
                admin.rollback();
            }
        }
    }

    @Test
    void assignmentsRequireOneDedicatedExamPerTenant() throws Exception {
        try (Connection admin = adminConnection();
             Statement st = admin.createStatement();
             ResultSet nullability = st.executeQuery(
                     "SELECT is_nullable "
                     + "FROM information_schema.columns "
                     + "WHERE table_schema = 'public' "
                     + "AND table_name = 'assignments' "
                     + "AND column_name = 'linked_exam_id'")) {
            assertThat(nullability.next())
                    .as("assignments.linked_exam_id exists")
                    .isTrue();
            assertThat(nullability.getString("is_nullable"))
                    .as("every assignment must reference an exam")
                    .isEqualTo("NO");

            try (ResultSet constraint = st.executeQuery(
                    "SELECT pg_get_constraintdef(c.oid) AS definition "
                    + "FROM pg_constraint c "
                    + "WHERE c.conrelid = 'assignments'::regclass "
                    + "AND c.conname = 'assignments_tenant_linked_exam_key' "
                    + "AND c.contype = 'u'")) {
                assertThat(constraint.next())
                        .as("one exam cannot back multiple assignments for the same tenant")
                        .isTrue();
                assertThat(constraint.getString("definition"))
                        .isEqualTo("UNIQUE (tenant_id, linked_exam_id)");
            }
        }
    }

    @Test
    void assignmentExamConstraintsRejectInvalidLinksAndAllowIndependentExams() throws Exception {
        long tenantA = 910_001L;
        long tenantB = 910_002L;
        long independentExam = 930_001L;
        long assignmentExam = 930_002L;
        long otherTenantExam = 930_003L;

        try (Connection admin = adminConnection();
             Statement st = admin.createStatement()) {
            insertExam(st, independentExam, tenantA, "independent-exam");
            insertExam(st, assignmentExam, tenantA, "assignment-exam");
            insertExam(st, otherTenantExam, tenantB, "other-tenant-exam");

            try (ResultSet assignments = st.executeQuery(
                    "SELECT count(*) FROM assignments "
                    + "WHERE tenant_id = " + tenantA + " "
                    + "AND linked_exam_id = " + independentExam)) {
                assignments.next();
                assertThat(assignments.getInt(1))
                        .as("an exam may exist independently of an assignment")
                        .isZero();
            }

            assertThatThrownBy(() -> insertAssignment(st, tenantA, null))
                    .as("an assignment without an exam is rejected")
                    .isInstanceOf(SQLException.class)
                    .satisfies(error -> assertThat(((SQLException) error).getSQLState())
                            .isEqualTo("23502"));

            insertAssignment(st, tenantA, assignmentExam);
            assertThatThrownBy(() -> insertAssignment(st, tenantA, assignmentExam))
                    .as("the same exam cannot back two assignments for one tenant")
                    .isInstanceOf(SQLException.class)
                    .satisfies(error -> assertThat(((SQLException) error).getSQLState())
                            .isEqualTo("23505"));

            assertThatThrownBy(() -> insertAssignment(st, tenantA, otherTenantExam))
                    .as("an assignment cannot reference another tenant's exam")
                    .isInstanceOf(SQLException.class)
                    .satisfies(error -> assertThat(((SQLException) error).getSQLState())
                            .isEqualTo("23503"));
        }
    }

    @Test
    void assignmentRecipientSnapshotsHaveTenantSafeOwnershipAndRls() throws Exception {
        try (Connection admin = adminConnection();
             Statement st = admin.createStatement();
             ResultSet columns = st.executeQuery(
                     "SELECT column_name, is_nullable "
                     + "FROM information_schema.columns "
                     + "WHERE table_schema = 'public' "
                     + "AND table_name = 'assignment_recipients'")) {
            var requiredColumns = new java.util.HashMap<String, String>();
            while (columns.next()) {
                requiredColumns.put(columns.getString("column_name"), columns.getString("is_nullable"));
            }
            assertThat(requiredColumns)
                    .containsEntry("assignment_id", "NO")
                    .containsEntry("student_id", "NO")
                    .containsEntry("tenant_id", "NO")
                    .containsEntry("version", "NO")
                    .containsEntry("created_at", "NO")
                    .containsEntry("updated_at", "NO");

            try (ResultSet constraints = st.executeQuery(
                    "SELECT conname, pg_get_constraintdef(oid) AS definition "
                    + "FROM pg_constraint "
                    + "WHERE conrelid = 'assignment_recipients'::regclass")) {
                var definitions = new java.util.HashMap<String, String>();
                while (constraints.next()) {
                    definitions.put(constraints.getString("conname"), constraints.getString("definition"));
                }
                assertThat(definitions)
                        .containsEntry("assignment_recipients_assignment_student_key",
                                "UNIQUE (assignment_id, student_id)")
                        .containsEntry("assignment_recipients_assignment_tenant_fkey",
                                "FOREIGN KEY (assignment_id, tenant_id) REFERENCES assignments(id, tenant_id) ON DELETE CASCADE");
            }

            try (ResultSet rls = st.executeQuery(
                    "SELECT relrowsecurity, relforcerowsecurity "
                    + "FROM pg_class WHERE oid = 'assignment_recipients'::regclass")) {
                assertThat(rls.next()).isTrue();
                assertThat(rls.getBoolean("relrowsecurity")).isTrue();
                assertThat(rls.getBoolean("relforcerowsecurity")).isTrue();
            }
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

    private static void insertExam(Statement statement, long examId, long tenantId, String name)
            throws Exception {
        statement.executeUpdate(
                "INSERT INTO exams (id, name, total_questions, correct_answers, question_scores, tenant_id) "
                + "VALUES (" + examId + ", '" + name + "', 1, '[1]'::jsonb, '[1]'::jsonb, "
                + tenantId + ")");
    }

    private static void insertClass(Statement statement, long classId, long tenantId, String name)
            throws Exception {
        statement.executeUpdate(
                "INSERT INTO classes (id, name, tenant_id) VALUES (" + classId + ", '"
                + name + "', " + tenantId + ")");
    }

    private static long insertReturningId(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }

    private static long countById(Statement statement, String table, long id) throws Exception {
        try (ResultSet result = statement.executeQuery(
                "SELECT count(*) FROM " + table + " WHERE id = " + id)) {
            assertThat(result.next()).isTrue();
            return result.getLong(1);
        }
    }

    private static void insertAssignment(Statement statement, long tenantId, Long examId)
            throws Exception {
        statement.executeUpdate(
                "INSERT INTO assignments (due_date, tenant_id, linked_exam_id) "
                + "VALUES (now() + interval '1 day', " + tenantId + ", "
                + (examId == null ? "null" : examId) + ")");
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
