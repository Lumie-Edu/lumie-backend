package com.lumie.auth.infrastructure.multitenancy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Hibernate multi-tenant connection provider that switches PostgreSQL schemas.
 * Uses the SCHEMA strategy where each tenant has their own schema within the same database.
 *
 * Note: Requires PgBouncer in session mode to maintain search_path across transactions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private static final String DEFAULT_SCHEMA = "public";

    private final DataSource dataSource;

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
        try {
            log.debug("Getting connection for tenant: {}", tenantIdentifier);
            setSearchPath(connection, tenantIdentifier);
            return connection;
        } catch (SQLException e) {
            log.error("Failed to set search_path for tenant: {}, closing connection", tenantIdentifier, e);
            connection.close();
            throw e;
        }
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        log.debug("Releasing connection for tenant: {}", tenantIdentifier);
        setSearchPath(connection, DEFAULT_SCHEMA);
        connection.close();
    }

    private void setSearchPath(Connection connection, String schema) throws SQLException {
        // Quote schema names to handle special characters like hyphens
        String quotedSchema = "\"" + schema + "\"";
        String searchPath = DEFAULT_SCHEMA.equals(schema) ? quotedSchema : quotedSchema + ", public";
        log.debug("Setting search_path to: {}", searchPath);
        try (var stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO " + searchPath);
        }
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
