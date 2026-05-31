package com.lumie.tenant.application.service;

import com.lumie.common.cache.CacheConfig;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.tenant.application.dto.response.PublicTenantResponse;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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

    public PublicTenantResponse getPublicTenantByCustomId(String customId) {
        return tenantPersistencePort.findByCustomId(customId)
                .filter(Tenant::isActive)
                .map(PublicTenantResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", customId));
    }

    /**
     * Looks up a public tenant by its custom domain.
     *
     * <p>Expects a canonical host: lowercased and trimmed. Normalization is the
     * caller's responsibility (performed at the controller boundary) so that the
     * {@code @Cacheable} key is always in canonical form.
     */
    @Cacheable(value = CacheConfig.CACHE_TENANTS_BY_DOMAIN, key = "#host")
    public PublicTenantResponse getPublicTenantByCustomDomain(String host) {
        return tenantPersistencePort.findByCustomDomain(host)
                .filter(Tenant::isActive)
                .map(PublicTenantResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", host));
    }
}
