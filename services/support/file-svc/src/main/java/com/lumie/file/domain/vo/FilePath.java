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
        String tenantId = extractTenantId(tenantSlug);
        return tenantId + "/" + entityType.getPathSegment() + "/" + filename;
    }

    private String extractTenantId(String tenantSlug) {
        // inst-c704d223 -> c704d223
        if (tenantSlug.startsWith("inst-")) {
            return tenantSlug.substring(5);
        }
        return tenantSlug;
    }
}
