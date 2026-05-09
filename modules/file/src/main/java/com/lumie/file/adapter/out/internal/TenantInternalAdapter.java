package com.lumie.file.adapter.out.internal;

import com.lumie.file.application.port.out.TenantServicePort;
import com.lumie.internal.tenant.TenantInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component("fileTenantInternalAdapter")
@RequiredArgsConstructor
public class TenantInternalAdapter implements TenantServicePort {

    private final TenantInternalService tenantInternalService;

    @Override
    public boolean validateTenant(String slug) {
        log.debug("Validating tenant via internal API: {}", slug);
        return tenantInternalService.validateTenant(slug).valid();
    }

    @Override
    public Optional<TenantData> getTenantBySlug(String slug) {
        log.debug("Getting tenant by slug via internal API: {}", slug);
        return tenantInternalService.getTenantBySlug(slug)
                .map(t -> new TenantData(t.id(), t.slug(), t.name(), t.status()));
    }
}
