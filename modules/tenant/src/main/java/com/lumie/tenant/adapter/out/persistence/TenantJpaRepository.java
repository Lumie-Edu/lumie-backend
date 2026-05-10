package com.lumie.tenant.adapter.out.persistence;

import com.lumie.tenant.domain.entity.Tenant;
import com.lumie.tenant.api.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenantJpaRepository extends JpaRepository<Tenant, Long> {

    @Query("SELECT t FROM Tenant t WHERE t.slug.value = :slug")
    Optional<Tenant> findBySlug(@Param("slug") String slug);

    @Query("SELECT COUNT(t) > 0 FROM Tenant t WHERE t.slug.value = :slug")
    boolean existsBySlug(@Param("slug") String slug);

    Optional<Tenant> findByCustomId(String customId);

    boolean existsByCustomId(String customId);

    Optional<Tenant> findByCustomDomain(String customDomain);

    boolean existsByCustomDomain(String customDomain);

    List<Tenant> findAllByStatus(TenantStatus status);
}
