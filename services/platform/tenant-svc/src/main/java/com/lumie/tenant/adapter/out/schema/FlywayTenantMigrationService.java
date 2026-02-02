package com.lumie.tenant.adapter.out.schema;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Service for running Flyway migrations on tenant schemas.
 * Each tenant schema has its own flyway_schema_history table to track migrations independently.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlywayTenantMigrationService {

    private final DataSource dataSource;

    private static final String TENANT_MIGRATION_LOCATION = "classpath:db/migration/tenant";
    private static final String PUBLIC_MIGRATION_LOCATION = "classpath:db/migration/public";

    /**
     * Run migrations for a specific tenant schema.
     * Creates flyway_schema_history in the tenant schema to track applied migrations.
     *
     * @param schemaName The tenant schema name (e.g., "tenant_xxx")
     */
    public void migrateSchema(String schemaName) {
        log.info("Running Flyway migrations for schema: {}", schemaName);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations(TENANT_MIGRATION_LOCATION)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .table("flyway_schema_history")
                .createSchemas(false)  // Schema is already created by TenantSchemaManager
                .load();

        flyway.migrate();
        log.info("Flyway migrations completed for schema: {}", schemaName);
    }

    /**
     * Run migrations for the public schema.
     * This includes the global users table.
     */
    public void migratePublicSchema() {
        log.info("Running Flyway migrations for public schema");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas("public")
                .locations(PUBLIC_MIGRATION_LOCATION)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .table("flyway_schema_history")
                .createSchemas(false)
                .load();

        flyway.migrate();
        log.info("Flyway migrations completed for public schema");
    }

    /**
     * Get the current migration info for a tenant schema.
     *
     * @param schemaName The tenant schema name
     * @return Flyway info object
     */
    public org.flywaydb.core.api.MigrationInfo[] getSchemaInfo(String schemaName) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations(TENANT_MIGRATION_LOCATION)
                .table("flyway_schema_history")
                .load();

        return flyway.info().all();
    }

    /**
     * Baseline an existing schema (for migrating from manual DDL to Flyway).
     * This marks V1 as already applied without actually running it.
     *
     * @param schemaName The tenant schema name
     */
    public void baselineSchema(String schemaName) {
        log.info("Baselining schema: {}", schemaName);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations(TENANT_MIGRATION_LOCATION)
                .baselineVersion("1")
                .baselineDescription("initial_tenant_schema")
                .table("flyway_schema_history")
                .createSchemas(false)
                .load();

        flyway.baseline();
        log.info("Schema baselined: {}", schemaName);
    }
}
