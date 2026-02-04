package com.lumie.common.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Hibernate multi-tenancy resolver that uses TenantContextHolder
 * to determine the current tenant identifier.
 */
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    private static final String DEFAULT_TENANT = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenant = TenantContextHolder.getTenant();
        return tenant != null ? tenant : DEFAULT_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
