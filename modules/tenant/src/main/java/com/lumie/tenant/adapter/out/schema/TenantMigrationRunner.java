package com.lumie.tenant.adapter.out.schema;

import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import com.lumie.tenant.domain.vo.TenantStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs Flyway migrations for all active tenant schemas on application startup.
 * This ensures all existing tenants get new migrations applied.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantMigrationRunner implements ApplicationRunner {

    private final TenantPersistencePort tenantPersistencePort;
    private final FlywayTenantMigrationService flywayMigrationService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting tenant schema migrations for all active tenants...");

        List<Tenant> activeTenants = tenantPersistencePort.findAllByStatus(TenantStatus.ACTIVE);
        log.info("Found {} active tenants to migrate", activeTenants.size());

        int successCount = 0;
        int failCount = 0;

        for (Tenant tenant : activeTenants) {
            String schemaName = tenant.getSchemaName();
            try {
                log.info("Migrating schema: {}", schemaName);
                flywayMigrationService.migrateSchema(schemaName);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to migrate schema: {}", schemaName, e);
                failCount++;
            }
        }

        log.info("Tenant schema migrations completed. Success: {}, Failed: {}", successCount, failCount);
    }
}
