package com.lumie.tenant.application.port.out;

import com.lumie.tenant.domain.entity.Tenant;
import com.lumie.tenant.api.TenantStatus;

import java.util.List;
import java.util.Optional;

public interface TenantPersistencePort {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(Long id);
    Optional<Tenant> findBySlug(String slug);
    Optional<Tenant> findByCustomId(String customId);
    Optional<Tenant> findByCustomDomain(String customDomain);
    boolean existsBySlug(String slug);
    boolean existsByCustomId(String customId);
    boolean existsByCustomDomain(String customDomain);
    List<Tenant> findAllByStatus(TenantStatus status);
    List<Tenant> findAll();
    void delete(Tenant tenant);
}
