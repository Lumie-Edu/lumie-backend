package com.lumie.tenant.adapter.out.schema;

import com.lumie.tenant.application.port.out.SchemaProvisioningPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages tenant schema lifecycle.
 * Schema migrations are handled by Flyway.
 *
 * Note: Users are stored in public.users with tenant_id for multi-tenant support.
 * Tenant schemas contain students, admins, etc. that reference public.users.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaManager implements SchemaProvisioningPort {

    private final DataSource dataSource;
    private final FlywayTenantMigrationService flywayMigrationService;

    private volatile boolean publicSchemaInitialized = false;

    @Override
    public void createSchema(String schemaName) {
        log.info("Creating schema: {}", schemaName);
        String sql = String.format("CREATE SCHEMA IF NOT EXISTS %s", sanitizeSchemaName(schemaName));
        executeWithAutoCommit(sql);
        log.info("Schema created: {}", schemaName);
    }

    @Override
    public void migrateSchema(String schemaName) {
        log.info("Migrating schema: {}", schemaName);
        String schema = sanitizeSchemaName(schemaName);

        // Ensure public.users table exists before creating tenant tables
        ensurePublicSchema();

        // Run Flyway migrations for the tenant schema
        flywayMigrationService.migrateSchema(schema);

        // Grant permissions (must be done after tables are created)
        grantPermissions(schema);

        log.info("Schema migration completed: {}", schemaName);
    }

    /**
     * Ensures the public schema has necessary tables (users).
     * Uses Flyway for migration tracking.
     */
    private synchronized void ensurePublicSchema() {
        if (publicSchemaInitialized) {
            return;
        }

        log.info("Ensuring public schema is initialized");
        flywayMigrationService.migratePublicSchema();

        // Grant permissions on public.users
        executeWithAutoCommit("GRANT ALL PRIVILEGES ON public.users TO lumie");
        try {
            executeWithAutoCommit("GRANT USAGE, SELECT ON SEQUENCE public.users_id_seq TO lumie");
        } catch (Exception e) {
            log.debug("Sequence grant may have failed (might not exist yet): {}", e.getMessage());
        }

        publicSchemaInitialized = true;
        log.info("Public schema initialized");
    }

    private void grantPermissions(String schema) {
        String[] grantStatements = {
            String.format("GRANT USAGE ON SCHEMA %s TO lumie", schema),
            String.format("GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA %s TO lumie", schema),
            String.format("GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA %s TO lumie", schema)
        };

        for (String sql : grantStatements) {
            executeWithAutoCommit(sql);
        }
        log.info("Granted permissions on schema: {}", schema);
    }

    @Override
    public void dropSchema(String schemaName) {
        log.warn("Dropping schema: {}", schemaName);
        String sql = String.format("DROP SCHEMA IF EXISTS %s CASCADE", sanitizeSchemaName(schemaName));
        executeWithAutoCommit(sql);
        log.warn("Schema dropped: {}", schemaName);
    }

    @Override
    public boolean schemaExists(String schemaName) {
        String sql = "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?";
        try (Connection conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check schema existence: " + schemaName, e);
        }
    }

    private void executeWithAutoCommit(String sql) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            conn.setAutoCommit(true);
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute DDL: " + sql, e);
        }
    }

    private String sanitizeSchemaName(String schemaName) {
        if (!schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
        return schemaName;
    }
}
