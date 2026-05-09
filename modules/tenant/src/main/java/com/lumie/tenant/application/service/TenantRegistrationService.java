package com.lumie.tenant.application.service;

import com.lumie.tenant.application.port.out.SchemaProvisioningPort;
import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantRegistrationService {

    private final TenantPersistencePort tenantPersistencePort;
    private final SchemaProvisioningPort schemaProvisioningPort;

    /**
     * Creates a tenant with synchronous schema provisioning.
     * Used for owner registration flow where immediate tenant activation is required.
     */
    @Transactional
    public TenantRegistrationResult registerTenant(String instituteName, String businessRegistrationNumber, String ownerEmail) {
        log.info("Starting tenant registration for institute: {}", instituteName);

        // 1. Create tenant with auto-generated slug
        Tenant tenant = Tenant.createWithAutoSlug(instituteName, businessRegistrationNumber, ownerEmail);
        Tenant savedTenant = tenantPersistencePort.save(tenant);
        log.info("Tenant created with slug: {}", savedTenant.getSlugValue());

        // 2. Start provisioning
        savedTenant.startProvisioning();
        tenantPersistencePort.save(savedTenant);

        String schemaName = savedTenant.getSchemaName();

        try {
            // 3. Create and migrate schema synchronously
            if (!schemaProvisioningPort.schemaExists(schemaName)) {
                schemaProvisioningPort.createSchema(schemaName);
                log.info("Schema created: {}", schemaName);
            }

            schemaProvisioningPort.migrateSchema(schemaName);
            log.info("Schema migration completed: {}", schemaName);

            // 4. Activate tenant
            savedTenant.activate();
            tenantPersistencePort.save(savedTenant);

            log.info("Tenant registration completed: {}", savedTenant.getSlugValue());

            return new TenantRegistrationResult(
                    true,
                    "Tenant registered successfully",
                    savedTenant.getId(),
                    savedTenant.getSlugValue(),
                    schemaName
            );

        } catch (Exception e) {
            log.error("Failed to provision schema for tenant: {}", savedTenant.getSlugValue(), e);
            throw new TenantRegistrationException("Schema provisioning failed: " + e.getMessage(), e);
        }
    }

    public record TenantRegistrationResult(
            boolean success,
            String message,
            Long tenantId,
            String tenantSlug,
            String schemaName
    ) {}

    public static class TenantRegistrationException extends RuntimeException {
        public TenantRegistrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
