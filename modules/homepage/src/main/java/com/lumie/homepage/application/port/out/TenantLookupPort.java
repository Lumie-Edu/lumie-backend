package com.lumie.homepage.application.port.out;

import java.util.Optional;

public interface TenantLookupPort {

    Optional<TenantInfo> findByCustomId(String customId);

    // tenantId is required for RLS: the public homepage read must set
    // `app.tenant_id` (not just the slug) or the tenant's rows stay invisible.
    record TenantInfo(String slug, Long tenantId) {}
}
