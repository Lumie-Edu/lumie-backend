package com.lumie.auth.infrastructure.multitenancy;

/**
 * ThreadLocal-based tenant context for multi-tenancy support.
 * Stores the current tenant's schema name for the duration of the request.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String schemaName) {
        CURRENT_TENANT.set(schemaName);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }
}
