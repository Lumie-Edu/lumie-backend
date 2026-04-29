package com.lumie.internal.tenant;

import java.util.Optional;

public interface TenantInternalService {

    Optional<TenantData> getTenant(Long tenantId);

    Optional<TenantData> getTenantBySlug(String slug);

    ValidateTenantResult validateTenant(String slug);

    CreateTenantResult createTenant(String instituteName, String businessRegistrationNumber,
                                    String ownerEmail, String ownerName);

    record TenantData(
            Long id,
            String slug,
            String name,
            String status,
            String schemaName,
            String customId,
            String createdAt,
            String updatedAt
    ) {}

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
