package com.lumie.tenant.adapter.out.schema;

import com.lumie.tenant.application.port.out.SchemaProvisioningPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaManager implements SchemaProvisioningPort {

    private final DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void createSchema(String schemaName) {
        log.info("Creating schema: {}", schemaName);
        String sanitized = sanitizeSchemaName(schemaName);
        entityManager.createNativeQuery("CREATE SCHEMA IF NOT EXISTS " + sanitized)
                .executeUpdate();
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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void dropSchema(String schemaName) {
        log.warn("Dropping schema: {}", schemaName);
        String sanitized = sanitizeSchemaName(schemaName);
        entityManager.createNativeQuery("DROP SCHEMA IF EXISTS " + sanitized + " CASCADE")
                .executeUpdate();
        log.warn("Schema dropped: {}", schemaName);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean schemaExists(String schemaName) {
        Long count = (Long) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = :schemaName")
                .setParameter("schemaName", schemaName)
                .getSingleResult();
        return count != null && count > 0;
    }

    private String sanitizeSchemaName(String schemaName) {
        if (!schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
        return schemaName;
    }
}
