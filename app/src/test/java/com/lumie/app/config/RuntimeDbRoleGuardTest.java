package com.lumie.app.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link RuntimeDbRoleGuard#verifyCannotBypassRls(Connection)}.
 *
 * <p>Uses a bare TestContainers Postgres (no Spring context) — the static method
 * under test accepts a plain {@link Connection}, so no application wiring is needed.
 * Lives in {@code com.lumie.app.config} (same package as the guard) to access the
 * package-private {@code verifyCannotBypassRls} method directly.
 *
 * <p>Two directions are tested:
 * <ol>
 *   <li><b>Pass</b> — a {@code NOSUPERUSER NOBYPASSRLS} role must not throw.</li>
 *   <li><b>Reject</b> — the container's default superuser must throw
 *       {@link IllegalStateException} with the bypass wording.</li>
 * </ol>
 */
@Tag("integration")
@Testcontainers
@Timeout(value = 90, unit = TimeUnit.SECONDS)
class RuntimeDbRoleGuardTest {

    private static final String RESTRICTED_ROLE = "guard_test_role";
    private static final String RESTRICTED_ROLE_PW = "guard_test_pw";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @BeforeAll
    static void createRestrictedRole() throws Exception {
        try (Connection admin = adminConnection();
             Statement st = admin.createStatement()) {
            st.execute("CREATE ROLE " + RESTRICTED_ROLE
                    + " LOGIN PASSWORD '" + RESTRICTED_ROLE_PW + "'"
                    + " NOSUPERUSER NOBYPASSRLS");
        }
    }

    /**
     * Verifies that the guard passes without throwing for a role that is
     * {@code NOSUPERUSER NOBYPASSRLS} — this is the expected shape of
     * {@code lumie_app} in all deployed environments.
     */
    @Test
    void runtimeRoleGuardPassesForRestrictedRole() throws Exception {
        try (Connection conn = restrictedRoleConnection()) {
            assertThatCode(() -> RuntimeDbRoleGuard.verifyCannotBypassRls(conn))
                    .as("guard must not throw for a NOSUPERUSER NOBYPASSRLS role")
                    .doesNotThrowAnyException();
        }
    }

    /**
     * Verifies that the guard throws {@link IllegalStateException} for a superuser,
     * and that the message identifies the bypass attribute so operators know what to fix.
     */
    @Test
    void runtimeRoleGuardRejectsSuperuser() throws Exception {
        try (Connection conn = adminConnection()) {
            assertThatThrownBy(() -> RuntimeDbRoleGuard.verifyCannotBypassRls(conn))
                    .as("guard must reject a superuser role (RLS would be bypassed)")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("RLS would be bypassed");
        }
    }

    private static Connection adminConnection() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static Connection restrictedRoleConnection() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), RESTRICTED_ROLE, RESTRICTED_ROLE_PW);
    }
}
