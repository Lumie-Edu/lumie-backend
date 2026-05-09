package com.lumie.file.application.port.out;

import java.util.Optional;

public interface TenantServicePort {

    boolean validateTenant(String slug);

    Optional<TenantData> getTenantBySlug(String slug);

    record TenantData(Long id, String slug, String name, String status) {
    }
}
