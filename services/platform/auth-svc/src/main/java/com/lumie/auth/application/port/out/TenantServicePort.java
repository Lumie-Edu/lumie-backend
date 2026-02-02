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
     * Validates a tenant by ID and returns tenant information if valid.
     *
     * @param tenantId the tenant ID
     * @return optional containing tenant data if valid
     */
    Optional<TenantData> validateTenantById(Long tenantId);

    /**
     * Creates a new tenant with synchronous schema provisioning.
     *
     * @param instituteName the institute name
     * @param businessRegistrationNumber the business registration number
     * @param ownerEmail the owner's email
     * @param ownerName the owner's name
     * @return the tenant creation result
     */
    TenantCreationResult createTenant(String instituteName, String businessRegistrationNumber,
                                       String ownerEmail, String ownerName);

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

    /**
     * Result of tenant creation.
     */
    record TenantCreationResult(
            boolean success,
            String message,
            Long tenantId,
            String tenantSlug,
            String schemaName
    ) {}
}
