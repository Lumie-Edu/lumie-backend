package com.lumie.tenant.application.service;

import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.tenant.application.dto.response.PublicTenantResponse;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantQueryService {

    private final TenantPersistencePort tenantPersistencePort;
    private final HostTenantResolver hostTenantResolver;

    public TenantResponse getTenantById(Long id) {
        return tenantPersistencePort.findById(id)
                .map(TenantResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
    }

    public TenantResponse getTenantBySlug(String slug) {
        return tenantPersistencePort.findBySlug(slug)
                .map(TenantResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", slug));
    }

    public List<TenantResponse> getAllTenants() {
        return tenantPersistencePort.findAll().stream()
                .map(TenantResponse::from)
                .toList();
    }

    public boolean isValidAndActive(String slug) {
        return tenantPersistencePort.findBySlug(slug)
                .map(Tenant::isActive)
                .orElse(false);
    }

    public Optional<Long> getTenantIdBySlug(String slug) {
        return tenantPersistencePort.findBySlug(slug)
                .map(Tenant::getId);
    }

    public PublicTenantResponse getCurrentPublicTenant(String slug) {
        return tenantPersistencePort.findBySlug(slug)
                .filter(Tenant::isActive)
                .map(tenant -> PublicTenantResponse.from(tenant, hostTenantResolver.loginThemeFor(tenant.getSlugValue())))
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", slug));
    }

}
