package com.lumie.common.tenant;

/**
 * Holds the current tenant context for the request thread.
 * Tenant slug is read from the X-Tenant-Slug header (set by the frontend
 * from session state) by {@code RequestContextFilter}.
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
     * Run an action with the tenant context temporarily set to the given slug
     * (and optional id), restoring the previous context afterward — even if
     * the action throws.
     *
     * <p>Use this whenever the caller needs to switch tenant context for a
     * scoped piece of work (e.g. cross-tenant lookups, internal service
     * adapters, scheduled jobs that iterate tenants). It replaces the
     * try/finally context-save/restore boilerplate that was duplicated across
     * the codebase.
     */
    public static <T> T withinContext(String tenantSlug, Long tenantId, java.util.function.Supplier<T> action) {
        String previousSlug = CURRENT_TENANT.get();
        Long previousId = CURRENT_TENANT_ID.get();
        try {
            if (tenantSlug != null) {
                CURRENT_TENANT.set(tenantSlug);
            }
            if (tenantId != null) {
                CURRENT_TENANT_ID.set(tenantId);
            }
            return action.get();
        } finally {
            if (previousSlug != null) {
                CURRENT_TENANT.set(previousSlug);
            } else {
                CURRENT_TENANT.remove();
            }
            if (previousId != null) {
                CURRENT_TENANT_ID.set(previousId);
            } else {
                CURRENT_TENANT_ID.remove();
            }
        }
    }

    /**
     * Convenience overload for callers that don't need to set tenantId
     * (it will be unchanged from the prior context).
     */
    public static <T> T withinContext(String tenantSlug, java.util.function.Supplier<T> action) {
        return withinContext(tenantSlug, null, action);
    }

    /**
     * Run a void action within the given tenant context.
     */
    public static void withinContext(String tenantSlug, Long tenantId, Runnable action) {
        withinContext(tenantSlug, tenantId, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Returns the storage-friendly tenant identifier derived from a slug —
     * stripping the well-known {@code "inst-"} prefix used for institute
     * tenants. Used as the top-level segment of object-storage keys (e.g.
     * MinIO paths under {@code <tenantId>/<entityType>/<filename>}).
     *
     * <p>Examples:
     * <pre>
     *   inst-c704d223  →  c704d223
     *   demo           →  demo
     * </pre>
     */
    public static String toStorageTenantId(String tenantSlug) {
        if (tenantSlug == null) return null;
        return tenantSlug.startsWith("inst-") ? tenantSlug.substring("inst-".length()) : tenantSlug;
    }
}
