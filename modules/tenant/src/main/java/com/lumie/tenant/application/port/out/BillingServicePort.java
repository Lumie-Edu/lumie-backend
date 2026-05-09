package com.lumie.tenant.application.port.out;

/**
 * Outgoing port to the billing module. Allows the tenant application service
 * to provision a default subscription on tenant creation without coupling to
 * the billing adapter implementation.
 */
public interface BillingServicePort {

    /**
     * Create the default (free) subscription for a newly registered tenant.
     */
    void createFreeSubscription(Long tenantId, String tenantSlug);
}
