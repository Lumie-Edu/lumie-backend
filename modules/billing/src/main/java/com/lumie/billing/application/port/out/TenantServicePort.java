package com.lumie.billing.application.port.out;

import java.util.Optional;

public interface TenantServicePort {

    record TenantData(Long id, String slug, String name, String status) {}

    Optional<TenantData> getTenantBySlug(String slug);
    Optional<TenantData> getTenantById(Long id);
    boolean validateTenant(String slug);
}
