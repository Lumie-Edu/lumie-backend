package com.lumie.tenant.application.service;

import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Synchronous tenant registration for owner sign-up. Post-RLS the flow is
 * just three persistence writes (PENDING → PROVISIONING → ACTIVE); no
 * schema provisioning, no Flyway runtime invocation. Kept as a dedicated
 * service rather than folding into {@link TenantCommandService} because
 * the owner-registration entry point still wants its own transaction
 * boundary and result DTO shape.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantRegistrationService {

    private final TenantPersistencePort tenantPersistencePort;

    @Transactional
    public TenantRegistrationResult registerTenant(String instituteName, String businessRegistrationNumber, String ownerEmail) {
        log.info("Starting tenant registration for institute: {}", instituteName);

        Tenant tenant = Tenant.createWithAutoSlug(instituteName, businessRegistrationNumber, ownerEmail);
        Tenant savedTenant = tenantPersistencePort.save(tenant);
        log.info("Tenant created with slug: {}", savedTenant.getSlugValue());

        savedTenant.startProvisioning();
        savedTenant.activate();
        tenantPersistencePort.save(savedTenant);

        log.info("Tenant registration completed: {}", savedTenant.getSlugValue());

        return new TenantRegistrationResult(
                true,
                "Tenant registered successfully",
                savedTenant.getId(),
                savedTenant.getSlugValue()
        );
    }

    public record TenantRegistrationResult(
            boolean success,
            String message,
            Long tenantId,
            String tenantSlug
    ) {}

    public static class TenantRegistrationException extends RuntimeException {
        public TenantRegistrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
