package com.lumie.tenant.application.port.out;

import com.lumie.tenant.domain.entity.Tenant;
import com.lumie.tenant.api.TenantStatus;

import java.util.List;
import java.util.Optional;

public interface TenantPersistencePort {
    Tenant save(Tenant tenant);
    Optional<Tenant> findById(Long id);
    Optional<Tenant> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Tenant> findAllByStatus(TenantStatus status);
    List<Tenant> findAll();
    void delete(Tenant tenant);
}
