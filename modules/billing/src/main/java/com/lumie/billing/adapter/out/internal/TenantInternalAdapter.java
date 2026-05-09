package com.lumie.billing.adapter.out.internal;

import com.lumie.billing.application.port.out.TenantServicePort;
import com.lumie.internal.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component("billingTenantInternalAdapter")
@RequiredArgsConstructor
public class TenantInternalAdapter implements TenantServicePort {

    private final TenantService tenantService;

    @Override
    public Optional<TenantData> getTenantBySlug(String slug) {
        log.debug("Getting tenant by slug via internal API: {}", slug);
        return tenantService.getTenantBySlug(slug)
                .map(t -> new TenantData(t.id(), t.slug(), t.name(), t.status()));
    }

    @Override
    public Optional<TenantData> getTenantById(Long id) {
        log.debug("Getting tenant by ID via internal API: {}", id);
        return tenantService.getTenant(id)
                .map(t -> new TenantData(t.id(), t.slug(), t.name(), t.status()));
    }

    @Override
    public boolean validateTenant(String slug) {
        log.debug("Validating tenant via internal API: {}", slug);
        return tenantService.validateTenant(slug).valid();
    }
}
