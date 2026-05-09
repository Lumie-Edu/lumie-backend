package com.lumie.tenant.application.port.in;

import java.util.Optional;

public interface ValidateTenantUseCase {
    boolean isValidAndActive(String slug);
    Optional<Long> getTenantIdBySlug(String slug);
}
