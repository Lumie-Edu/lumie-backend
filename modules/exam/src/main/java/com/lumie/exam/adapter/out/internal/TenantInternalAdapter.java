package com.lumie.exam.adapter.out.internal;

import com.lumie.exam.application.port.out.TenantServicePort;
import com.lumie.internal.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component("examTenantInternalAdapter")
@RequiredArgsConstructor
public class TenantInternalAdapter implements TenantServicePort {

    private final TenantService tenantService;

    @Override
    public boolean validateTenant(String slug) {
        log.debug("Validating tenant via internal API: {}", slug);
        return tenantService.validateTenant(slug).valid();
    }

    @Override
    public Optional<TenantData> getTenantBySlug(String slug) {
        log.debug("Getting tenant by slug via internal API: {}", slug);
        return tenantService.getTenantBySlug(slug)
                .map(t -> new TenantData(t.id(), t.slug(), t.name(), t.status()));
    }
}
