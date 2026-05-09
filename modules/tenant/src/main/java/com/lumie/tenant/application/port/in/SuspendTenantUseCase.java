package com.lumie.tenant.application.port.in;

import com.lumie.tenant.application.dto.response.TenantResponse;

public interface SuspendTenantUseCase {
    TenantResponse suspendTenant(String slug, String reason);
    TenantResponse reactivateTenant(String slug);
}
