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
 * DDL operations use direct connections with auto-commit to avoid transaction issues.
 * Schema migrations are managed manually - Flyway has been removed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaManager implements SchemaProvisioningPort {

    private final DataSource dataSource;

    @Override
    public void createSchema(String schemaName) {
        log.info("Creating schema: {}", schemaName);
        String sql = String.format("CREATE SCHEMA IF NOT EXISTS %s", sanitizeSchemaName(schemaName));
        executeWithAutoCommit(sql);
        log.info("Schema created: {}", schemaName);
    }

    @Override
    public void migrateSchema(String schemaName) {
        // Flyway removed - migrations are now managed manually
        log.info("Schema migration skipped (manual management): {}", schemaName);
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
