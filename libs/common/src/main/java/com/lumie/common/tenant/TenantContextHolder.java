package com.lumie.common.tenant;

/**
 * Holds the current tenant context for the request thread.
 * Tenant slug is extracted from X-Tenant-Slug header set by Kong JWT plugin.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_TENANT_ID = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setTenant(String tenantSlug) {
        CURRENT_TENANT.set(tenantSlug);
    }

    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT_ID.set(tenantId);
    }

    public static String getTenant() {
        return CURRENT_TENANT.get();
    }

    public static Long getTenantId() {
        return CURRENT_TENANT_ID.get();
    }

    public static Long getRequiredTenantId() {
        Long tenantId = CURRENT_TENANT_ID.get();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is not set in context");
        }
        return tenantId;
    }

    public static String getTenantSlug() {
        return getTenant();
    }

    public static String getRequiredTenant() {
        String tenant = CURRENT_TENANT.get();
        if (tenant == null || tenant.isBlank()) {
            throw new IllegalStateException("Tenant context not set");
        }
        return tenant;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_TENANT_ID.remove();
    }

    /**
     * Returns the PostgreSQL schema name for the current tenant.
     * Format: "tenant_" + slug with hyphens replaced by underscores.
     *
     * @return schema name, or "public" if no tenant is set
     */
    public static String getSchemaName() {
        String tenant = getTenant();
        if (tenant == null || tenant.isBlank()) {
            return "public";
        }
        return "tenant_" + tenant.replace("-", "_");
    }
}
