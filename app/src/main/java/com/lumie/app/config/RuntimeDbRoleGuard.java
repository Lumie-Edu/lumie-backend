package com.lumie.app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Boot-time guard that refuses to start if the runtime DB role can bypass RLS.
 *
 * <p>Postgres {@code FORCE ROW LEVEL SECURITY} does not bind superusers or roles
 * with {@code BYPASSRLS}. If the {@code lumie_app} runtime pool were accidentally
 * provisioned with either attribute, every multi-tenant RLS policy would be silently
 * ignored — all tenants' data would be accessible to all requests (multi-tenant data
 * leak). This guard detects that misconfiguration at context refresh and refuses to
 * start the application, preventing a silent leak from ever reaching traffic.
 *
 * <p>Disabled in full-context tests via
 * {@code lumie.security.runtime-role-guard.enabled=false} (TestContainers provisions
 * a superuser by default; the guard is a deployed-environment safeguard). See
 * {@link com.lumie.app.FullContextTestBase} and
 * {@link RoutingDataSourceIntegrationTest}.
 *
 * <h2>Why {@code IllegalStateException} instead of {@code BusinessException}</h2>
 *
 * <p>The {@code BusinessException(ErrorCode)} rule from repo AGENTS.md / HARD_RULES_CROSS.md
 * applies to domain-layer / request-handling errors (i.e., things that produce an
 * HTTP 4xx/5xx response to a client). This guard runs at application startup — before
 * any request is ever served — and signals infrastructure misconfiguration, not a
 * domain violation. {@code IllegalStateException} is the correct idiomatic choice for
 * "the application context is in an illegal state and cannot start", consistent with
 * how Spring itself signals misconfigured beans at boot time.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "lumie.security.runtime-role-guard.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class RuntimeDbRoleGuard implements InitializingBean {

    /** The {@code @Primary} routing DataSource — backed by the primary Hikari pool. */
    private final DataSource dataSource;

    @Override
    public void afterPropertiesSet() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            verifyCannotBypassRls(conn);
        }
    }

    /**
     * Queries {@code pg_roles} for the current connection's role and fails if it
     * has {@code SUPERUSER} or {@code BYPASSRLS}. Package-private so unit tests
     * in this package can call it directly with a pre-built connection.
     *
     * @param conn an open JDBC connection whose {@code current_user} is the role to verify
     * @throws IllegalStateException if the role can bypass RLS, or if the role is not
     *                               found / the query fails (fail-closed: unverifiable → refuse)
     */
    static void verifyCannotBypassRls(Connection conn) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT current_user AS role_name, rolsuper, rolbypassrls "
                     + "FROM pg_roles WHERE rolname = current_user")) {

            if (!rs.next()) {
                // The role is not in pg_roles — this should never happen for a valid
                // connected session, but fail-closed: cannot verify → refuse to start.
                throw new IllegalStateException(
                        "Runtime DB role guard: could not find current_user in pg_roles. "
                        + "Cannot verify NOSUPERUSER/NOBYPASSRLS — refusing to start.");
            }

            String roleName  = rs.getString("role_name");
            boolean isSuper  = rs.getBoolean("rolsuper");
            boolean bypassRls = rs.getBoolean("rolbypassrls");

            if (isSuper || bypassRls) {
                String attrs = buildBypassAttrs(isSuper, bypassRls);
                throw new IllegalStateException(
                        "Runtime DB role '" + roleName + "' has " + attrs
                        + " — RLS would be bypassed (multi-tenant data leak). "
                        + "Refusing to start. Use a NOSUPERUSER NOBYPASSRLS role (lumie_app).");
            }

            log.info("Runtime DB role '{}' verified NOSUPERUSER NOBYPASSRLS.", roleName);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            // Any SQLException or unexpected failure → fail-closed.
            throw new IllegalStateException(
                    "Runtime DB role guard: failed to query pg_roles — "
                    + "cannot verify NOSUPERUSER/NOBYPASSRLS. Refusing to start.", e);
        }
    }

    private static String buildBypassAttrs(boolean isSuper, boolean bypassRls) {
        if (isSuper && bypassRls) return "SUPERUSER and BYPASSRLS";
        if (isSuper)              return "SUPERUSER";
        return "BYPASSRLS";
    }
}
