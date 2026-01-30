package com.lumie.auth.application.port.out;

import java.util.Optional;

/**
 * Output port for tenant-svc gRPC client.
 */
public interface TenantServicePort {

    /**
     * Validates a tenant and returns tenant information if valid.
     *
     * @param slug the tenant slug
     * @return optional containing tenant data if valid
     */
    Optional<TenantData> validateTenant(String slug);

    /**
     * Gets tenant by slug.
     *
     * @param slug the tenant slug
     * @return optional containing tenant data if found
     */
    Optional<TenantData> getTenantBySlug(String slug);

    /**
     * Tenant data from tenant-svc.
     */
    record TenantData(
            Long id,
            String slug,
            String name,
            String schemaName,
            String status
    ) {
        public boolean isActive() {
            return "ACTIVE".equalsIgnoreCase(status);
        }
    }
}
