package com.lumie.tenant.api;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TenantService {

    Optional<TenantData> getTenant(Long tenantId);

    Optional<TenantData> getTenantBySlug(String slug);

    /**
     * Slugs of all tenants in {@link TenantStatus#ACTIVE} state. Used by
     * cross-tenant background jobs (schedulers, sweepers) instead of querying
     * {@code information_schema.schemata} directly, so DELETED/SUSPENDED
     * tenants are skipped and the registry stays the single source of truth.
     */
    List<String> listActiveTenantSlugs();

    /**
     * All ACTIVE tenants with both id and slug populated. Preferred over
     * {@link #listActiveTenantSlugs()} for scheduler/sweeper loops post-
     * RLS — both fields are needed in the per-iteration
     * {@link com.lumie.common.tenant.TenantContextHolder} setup
     * (the RLS aspect reads the id, audit logging / MDC reads the slug).
     * Avoids the N+1 slug→id lookup that would otherwise need a
     * per-iteration {@link #getTenantBySlug(String)} call.
     */
    List<TenantData> listActiveTenants();

    /**
     * Look up a tenant by its public-facing customId (the URL slug used in
     * {@code lumie-edu.com/{customId}}). Used by homepage-svc to route public
     * portal requests to the correct tenant context. Returns empty if no such
     * tenant exists or it is not active.
     */
    Optional<TenantData> getTenantByCustomId(String customId);

    ValidateTenantResult validateTenant(String slug);

    CreateTenantResult createTenant(String instituteName, String businessRegistrationNumber,
                                    String ownerEmail, String ownerName);

    void updatePlan(String tenantSlug, String planId);

    record TenantData(
            Long id,
            String slug,
            String name,
            TenantStatus status,
            String customId,
            Instant onboardingCompletedAt,
            String createdAt,
            String updatedAt
    ) {
        public boolean isActive() {
            return status == TenantStatus.ACTIVE;
        }
    }

    record ValidateTenantResult(
            boolean valid,
            String message,
            Long tenantId
    ) {}

    record CreateTenantResult(
            boolean success,
            String message,
            Long tenantId,
            String tenantSlug
    ) {}
}
