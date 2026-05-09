package com.lumie.tenant.application.port.in;

import com.lumie.tenant.application.dto.request.UpdateTenantRequest;
import com.lumie.tenant.application.dto.response.TenantResponse;

public interface UpdateTenantUseCase {
    TenantResponse updateTenant(String slug, UpdateTenantRequest request);
}
