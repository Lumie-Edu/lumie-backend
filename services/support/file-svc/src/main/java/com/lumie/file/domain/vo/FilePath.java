package com.lumie.file.domain.vo;

import java.util.UUID;

public record FilePath(
        String tenantSlug,
        EntityType entityType,
        UUID fileId,
        String filename
) {
    private static final String TENANT_PREFIX = "tenants";
    private static final String PLATFORM_PREFIX = "platform/logos";

    public static FilePath of(String tenantSlug, EntityType entityType, UUID fileId, String filename) {
        return new FilePath(tenantSlug, entityType, fileId, filename);
    }

    public static FilePath forPlatformLogo(String filename) {
        return new FilePath(null, EntityType.LOGO, null, filename);
    }

    public String toObjectKey() {
        if (entityType == EntityType.LOGO) {
            return PLATFORM_PREFIX + "/" + filename;
        }
        return TENANT_PREFIX + "/" + tenantSlug + "/" + entityType.getPathSegment() + "/" + fileId + "/" + filename;
    }

    public boolean isPlatformFile() {
        return entityType == EntityType.LOGO;
    }
}
