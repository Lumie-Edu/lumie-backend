package com.lumie.tenant.application.service;

import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.tenant.application.dto.request.TenantSettingsRequest;
import com.lumie.tenant.application.dto.response.TenantSettingsResponse;
import com.lumie.tenant.application.port.in.GetTenantSettingsUseCase;
import com.lumie.tenant.application.port.in.UpdateTenantSettingsUseCase;
import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import com.lumie.tenant.domain.entity.TenantSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantSettingsApplicationService implements GetTenantSettingsUseCase, UpdateTenantSettingsUseCase {

    private final TenantPersistencePort tenantPersistencePort;

    @Override
    @Transactional
    public TenantSettingsResponse getSettings(String slug) {
        log.debug("Getting settings for tenant: {}", slug);

        Tenant tenant = findTenantOrThrow(slug);
        TenantSettings settings = tenant.getOrCreateSettings();
        tenantPersistencePort.save(tenant);

        return TenantSettingsResponse.from(settings);
    }

    @Override
    @Transactional
    public TenantSettingsResponse updateSettings(String slug, TenantSettingsRequest request) {
        log.info("Updating settings for tenant: {}", slug);

        Tenant tenant = findTenantOrThrow(slug);
        TenantSettings settings = tenant.getOrCreateSettings();
        settings.update(request.logoUrl(), request.theme());
        tenantPersistencePort.save(tenant);

        log.info("Settings updated for tenant: {}", slug);
        return TenantSettingsResponse.from(settings);
    }

    private Tenant findTenantOrThrow(String slug) {
        return tenantPersistencePort.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", slug));
    }
}
