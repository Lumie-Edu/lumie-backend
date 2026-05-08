package com.lumie.homepage.adapter.out.internal;

import com.lumie.homepage.application.port.out.TenantLookupPort;
import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves a tenant by its customId using the tenant module's persistence port directly.
 * This is an in-process call — valid in the monolith since both modules share the classpath.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantLookupAdapter implements TenantLookupPort {

    private final TenantPersistencePort tenantPersistencePort;

    @Override
    public Optional<TenantInfo> findByCustomId(String customId) {
        return tenantPersistencePort.findByCustomId(customId)
                .filter(Tenant::isActive)
                .map(tenant -> new TenantInfo(tenant.getSlugValue(), tenant.getSchemaName()));
    }
}
