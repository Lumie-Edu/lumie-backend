package com.lumie.exam.infrastructure.tenant;

public final class TenantContextHolder {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setTenant(String tenantSlug) {
        CURRENT_TENANT.set(tenantSlug);
    }

    public static String getTenant() {
        return CURRENT_TENANT.get();
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
    }

    public static String getSchemaName() {
        String tenant = getTenant();
        if (tenant == null || tenant.isBlank()) {
            return "public";
        }
        return "tenant_" + tenant.replace("-", "_");
    }
}
