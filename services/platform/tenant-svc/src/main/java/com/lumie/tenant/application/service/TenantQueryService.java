package com.lumie.tenant.application.service;

import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.application.port.in.GetTenantUseCase;
import com.lumie.tenant.application.port.in.ValidateTenantUseCase;
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
public class TenantQueryService implements GetTenantUseCase, ValidateTenantUseCase {

    private final TenantPersistencePort tenantPersistencePort;

    @Override
    public TenantResponse getTenantById(Long id) {
        return tenantPersistencePort.findById(id)
                .map(TenantResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", id));
    }

    @Override
    public TenantResponse getTenantBySlug(String slug) {
        return tenantPersistencePort.findBySlug(slug)
                .map(TenantResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", slug));
    }

    @Override
    public List<TenantResponse> getAllTenants() {
        return tenantPersistencePort.findAll().stream()
                .map(TenantResponse::from)
                .toList();
    }

    @Override
    public boolean isValidAndActive(String slug) {
        return tenantPersistencePort.findBySlug(slug)
                .map(Tenant::isActive)
                .orElse(false);
    }

    @Override
    public Optional<Long> getTenantIdBySlug(String slug) {
        return tenantPersistencePort.findBySlug(slug)
                .map(Tenant::getId);
    }
}
