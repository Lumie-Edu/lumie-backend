package com.lumie.tenant.adapter.out.schema;

import com.lumie.tenant.application.port.out.SchemaProvisioningPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Manages tenant schema lifecycle using JdbcTemplate for DDL operations.
 * DDL operations (CREATE/DROP SCHEMA) are auto-commit and don't require transaction management.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaManager implements SchemaProvisioningPort {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void createSchema(String schemaName) {
        log.info("Creating schema: {}", schemaName);
        String sql = String.format("CREATE SCHEMA IF NOT EXISTS %s", sanitizeSchemaName(schemaName));
        jdbcTemplate.execute(sql);
        log.info("Schema created: {}", schemaName);
    }

    @Override
    public void migrateSchema(String schemaName) {
        log.info("Running migrations for schema: {}", schemaName);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/tenant-migration")
                .baselineOnMigrate(true)
                .table("flyway_schema_history")
                .load();

        flyway.migrate();
        log.info("Migrations completed for schema: {}", schemaName);
    }

    @Override
    public void dropSchema(String schemaName) {
        log.warn("Dropping schema: {}", schemaName);
        String sql = String.format("DROP SCHEMA IF EXISTS %s CASCADE", sanitizeSchemaName(schemaName));
        jdbcTemplate.execute(sql);
        log.warn("Schema dropped: {}", schemaName);
    }

    @Override
    public boolean schemaExists(String schemaName) {
        String sql = "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, schemaName);
        return count != null && count > 0;
    }

    private String sanitizeSchemaName(String schemaName) {
        if (!schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
        return schemaName;
    }
}
