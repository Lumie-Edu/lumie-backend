package com.lumie.tenant.adapter.out.persistence;

import com.lumie.tenant.application.port.out.TenantPersistencePort;
import com.lumie.tenant.domain.entity.Tenant;
import com.lumie.tenant.domain.vo.TenantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TenantPersistenceAdapter implements TenantPersistencePort {

    private final TenantJpaRepository tenantJpaRepository;

    @Override
    public Tenant save(Tenant tenant) {
        return tenantJpaRepository.save(tenant);
    }

    @Override
    public Optional<Tenant> findById(Long id) {
        return tenantJpaRepository.findById(id);
    }

    @Override
    public Optional<Tenant> findBySlug(String slug) {
        return tenantJpaRepository.findBySlug(slug);
    }

    @Override
    public Optional<Tenant> findByCustomId(String customId) {
        return tenantJpaRepository.findByCustomId(customId);
    }

    @Override
    public Optional<Tenant> findByCustomDomain(String customDomain) {
        return tenantJpaRepository.findByCustomDomain(customDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return tenantJpaRepository.existsBySlug(slug);
    }

    @Override
    public boolean existsByCustomId(String customId) {
        return tenantJpaRepository.existsByCustomId(customId);
    }

    @Override
    public boolean existsByCustomDomain(String customDomain) {
        return tenantJpaRepository.existsByCustomDomain(customDomain);
    }

    @Override
    public List<Tenant> findAllByStatus(TenantStatus status) {
        return tenantJpaRepository.findAllByStatus(status);
    }

    @Override
    public List<Tenant> findAll() {
        return tenantJpaRepository.findAll();
    }

    @Override
    public void delete(Tenant tenant) {
        tenantJpaRepository.delete(tenant);
    }
}
