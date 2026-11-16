package com.lumie.tenant.application.service;

import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Creates and activates a tenant within the owner-signup transaction. The auth
 * use case owns synchronous billing and exam initialization through their
 * internal APIs before the surrounding transaction commits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantRegistrationService {

    private final TenantPersistencePort tenantPersistencePort;

    @Transactional
    public TenantRegistrationResult registerTenant(String instituteName) {
        return registerTenant(instituteName, List.of());
    }

    @Transactional
    public TenantRegistrationResult registerTenant(String instituteName, List<String> subjects) {
        log.info("Starting tenant registration for institute: {}", instituteName);

        Tenant tenant = Tenant.createWithAutoSlug(instituteName, subjects);
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
