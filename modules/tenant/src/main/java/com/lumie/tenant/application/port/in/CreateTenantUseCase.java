package com.lumie.tenant.application.port.in;

import com.lumie.tenant.application.dto.request.CreateTenantRequest;
import com.lumie.tenant.application.dto.response.TenantResponse;

public interface CreateTenantUseCase {
    TenantResponse createTenant(CreateTenantRequest request);
}
