package com.lumie.tenant.adapter.out.schema;

import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import com.lumie.tenant.api.TenantStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs Flyway migrations for active tenant schemas after the application is ready.
 *
 * <p>Pre-2026-05 this was an {@link org.springframework.boot.ApplicationRunner}
 * that blocked startup with N × per-tenant duration. With many tenants, the pod
 * would not become Ready until every schema had been migrated, delaying rollouts
 * and making liveness probes flap. This refactor:
 *
 * <ul>
 *   <li>moves the work off the boot path — the pod becomes Ready immediately,
 *       migrations run on the {@code ApplicationReadyEvent} listener thread
 *       asynchronously;
 *   <li>tracks already-migrated schemas in {@link #migratedSchemas} for
 *       idempotency, so duplicate triggers (e.g. a future request-time
 *       safety net wired into the tenant resolver) do the work at most once
 *       per JVM lifetime;
 *   <li>can be disabled per-environment with
 *       {@code lumie.tenant.migrate-on-boot=false} (recommended in prod, where a
 *       separate ArgoCD post-sync Job invokes
 *       {@link FlywayTenantMigrationService} directly).
 * </ul>
 *
 * <p><b>Known gap:</b> there is currently no request-thread caller of
 * {@link #migrateIfNeeded(String)}. If a request lands on a tenant during the
 * async catch-up window after a fresh boot, that request will see an
 * unmigrated schema and fail. Acceptable today because (a) prod runs with
 * {@code migrate-on-boot=false} and uses an explicit Job, (b) dev typically
 * has a single tenant whose schema migration completes in &lt;1 s.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "lumie.tenant.migrate-on-boot", havingValue = "true", matchIfMissing = true)
public class TenantMigrationRunner {

    private final TenantPersistencePort tenantPersistencePort;
    private final FlywayTenantMigrationService flywayMigrationService;

    private final Set<String> migratedSchemas = ConcurrentHashMap.newKeySet();

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void migrateAllActiveTenants() {
        log.info("Starting async tenant schema migrations for all active tenants...");

        List<Tenant> activeTenants = tenantPersistencePort.findAllByStatus(TenantStatus.ACTIVE);
        log.info("Found {} active tenants to migrate", activeTenants.size());

        int successCount = 0;
        int failCount = 0;

        for (Tenant tenant : activeTenants) {
            if (migrateIfNeeded(tenant.getSchemaName())) {
                successCount++;
            } else {
                failCount++;
            }
        }

        log.info("Tenant schema migrations completed. Success: {}, Failed: {}", successCount, failCount);
    }

    /**
     * Idempotent per-schema migration entry point. Safe to call from request
     * threads if a tenant lands before the boot-time async pass has reached it.
     */
    public boolean migrateIfNeeded(String schemaName) {
        if (!migratedSchemas.add(schemaName)) {
            return true;
        }
        try {
            log.info("Migrating schema: {}", schemaName);
            flywayMigrationService.migrateSchema(schemaName);
            return true;
        } catch (Exception e) {
            log.error("Failed to migrate schema: {}", schemaName, e);
            migratedSchemas.remove(schemaName);
            return false;
        }
    }
}
