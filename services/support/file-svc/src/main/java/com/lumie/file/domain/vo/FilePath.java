package com.lumie.file.domain.vo;

import com.lumie.common.tenant.TenantContextHolder;

import java.util.UUID;

public record FilePath(
        EntityType entityType,
        UUID fileId,
        String filename
) {
    public static FilePath of(EntityType entityType, UUID fileId, String filename) {
        return new FilePath(entityType, fileId, filename);
    }

    public String toObjectKey() {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        String tenantId = TenantContextHolder.toStorageTenantId(tenantSlug);
        return tenantId + "/" + entityType.getPathSegment() + "/" + filename;
    }
}
