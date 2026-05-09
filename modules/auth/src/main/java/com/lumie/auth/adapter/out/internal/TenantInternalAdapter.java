package com.lumie.auth.adapter.out.internal;

import com.lumie.auth.application.port.out.TenantServicePort;
import com.lumie.internal.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component("authTenantInternalAdapter")
@RequiredArgsConstructor
public class TenantInternalAdapter implements TenantServicePort {

    private final TenantService tenantService;

    @Override
    public Optional<TenantData> validateTenant(String slug) {
        log.debug("Validating tenant via internal API: {}", slug);
        var result = tenantService.validateTenant(slug);
        if (!result.valid()) {
            log.debug("Tenant validation failed: {}", result.message());
            return Optional.empty();
        }
        return getTenantBySlug(slug);
    }

    @Override
    public Optional<TenantData> getTenantBySlug(String slug) {
        log.debug("Getting tenant by slug via internal API: {}", slug);
        return tenantService.getTenantBySlug(slug)
                .map(t -> new TenantData(t.id(), t.slug(), t.name(), t.schemaName(), t.status(), t.customId()));
    }

    @Override
    public Optional<TenantData> validateTenantById(Long tenantId) {
        log.debug("Validating tenant by ID via internal API: {}", tenantId);
        return tenantService.getTenant(tenantId)
                .map(t -> new TenantData(t.id(), t.slug(), t.name(), t.schemaName(), t.status(), t.customId()));
    }

    @Override
    public TenantCreationResult createTenant(String instituteName, String businessRegistrationNumber,
                                              String ownerEmail, String ownerName) {
        log.debug("Creating tenant via internal API: instituteName={}, ownerEmail={}", instituteName, ownerEmail);
        var result = tenantService.createTenant(instituteName, businessRegistrationNumber, ownerEmail, ownerName);
        return new TenantCreationResult(result.success(), result.message(), result.tenantId(), result.tenantSlug(), result.schemaName());
    }
}
