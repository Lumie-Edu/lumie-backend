package com.lumie.internal.tenant;

import java.util.Optional;

public interface TenantService {

    Optional<TenantData> getTenant(Long tenantId);

    Optional<TenantData> getTenantBySlug(String slug);

    /**
     * Look up a tenant by its public-facing customId (the URL slug used in
     * {@code lumie-edu.com/{customId}}). Used by homepage-svc to route public
     * portal requests to the correct tenant schema. Returns empty if no such
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
            String status,
            String schemaName,
            String customId,
            String createdAt,
            String updatedAt
    ) {
        public boolean isActive() {
            return "ACTIVE".equalsIgnoreCase(status);
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
            String tenantSlug,
            String schemaName
    ) {}
}
