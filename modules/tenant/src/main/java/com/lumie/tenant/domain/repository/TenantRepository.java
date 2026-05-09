package com.lumie.tenant.domain.repository;

import com.lumie.tenant.domain.entity.Tenant;
import com.lumie.tenant.domain.vo.TenantStatus;

import java.util.List;
import java.util.Optional;

public interface TenantRepository {

    Tenant save(Tenant tenant);

    Optional<Tenant> findById(Long id);

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Tenant> findAllByStatus(TenantStatus status);

    List<Tenant> findAll();

    void delete(Tenant tenant);
}
