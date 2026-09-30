package com.lumie.file.domain.vo;

import com.lumie.file.api.FileObjectKeyFactory;
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
        return FileObjectKeyFactory.file(tenantId, entityType.getPathSegment(), fileId, filename);
    }

    public String toTemporaryObjectKey() {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        String tenantId = TenantContextHolder.toStorageTenantId(tenantSlug);
        return FileObjectKeyFactory.temporaryUpload(tenantId, fileId, filename);
    }
}
