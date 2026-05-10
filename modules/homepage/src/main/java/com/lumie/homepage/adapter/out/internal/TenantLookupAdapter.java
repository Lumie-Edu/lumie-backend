package com.lumie.homepage.adapter.out.internal;

import com.lumie.homepage.application.port.out.TenantLookupPort;
import com.lumie.tenant.api.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resolves a tenant by its customId via the tenant module's published
 * internal-api boundary. This stays on the proper hexagonal coupling path —
 * homepage-svc consumes a public {@link TenantService} contract
 * instead of reaching into tenant-svc's persistence port + domain entity.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantLookupAdapter implements TenantLookupPort {

    private final TenantService tenantService;

    @Override
    public Optional<TenantInfo> findByCustomId(String customId) {
        return tenantService.getTenantByCustomId(customId)
                .map(t -> new TenantInfo(t.slug(), t.schemaName()));
    }
}
