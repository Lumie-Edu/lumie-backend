package com.lumie.common.tenant;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Hibernate {@link MultiTenantConnectionProvider} that scopes each acquired
 * connection to the current tenant's Postgres schema.
 *
 * <p>Uses {@code SELECT set_config('search_path', …, true)} — the
 * {@code is_local=true} flag scopes the override to the current
 * <strong>transaction</strong>, not the session. This is the pattern that
 * survives PgBouncer transaction-pool mode: PgBouncer pins the backend
 * connection from {@code BEGIN} to {@code COMMIT}, so a tx-local
 * search_path is guaranteed to apply to every statement Hibernate issues
 * within that tx, and resets automatically when the tx ends — even if the
 * physical connection is recycled to a different client afterwards.
 *
 * <p>The earlier {@code SET search_path} (session-level) version drifted
 * across pool boundaries: setting then a no-op release-time reset both
 * ran outside an explicit tx, and PgBouncer would reassign the backend
 * before the next tx, leaving the next query reading from {@code public}.
 *
 * <p>Implication: any database access path that opens a Hibernate session
 * <em>without</em> a Spring {@code @Transactional} boundary will see the
 * default {@code public} search_path. All Lumie services follow the
 * {@code @Transactional}-on-services rule (BACKEND_RULES) so this is the
 * correct trade-off.
 */
@Slf4j
@Component
public class TenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private static final String DEFAULT_SCHEMA = "public";
    private static final String SET_SEARCH_PATH_SQL = "SELECT set_config('search_path', ?, true)";

    private final DataSource dataSource;

    public TenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = dataSource.getConnection();
        String schema = TenantContextHolder.getSchemaName();

        if (DEFAULT_SCHEMA.equals(schema)) {
            // Public schema needs no override — Postgres default search_path
            // already includes it.
            return connection;
        }

        log.debug("Setting tx-local schema for tenant {}: {}", tenantIdentifier, schema);
        try (PreparedStatement stmt = connection.prepareStatement(SET_SEARCH_PATH_SQL)) {
            stmt.setString(1, schema + ", public");
            stmt.executeQuery();
        } catch (SQLException e) {
            log.error("Failed to set schema: {}", schema, e);
            connection.close();
            throw e;
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        // No reset needed — set_config(..., true) is tx-local and PgBouncer
        // recycles the backend connection at COMMIT in transaction-pool mode.
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        throw new UnsupportedOperationException("Cannot unwrap to " + unwrapType);
    }
}
