package com.lumie.tenant.application.port.in;

import com.lumie.tenant.application.dto.response.TenantResponse;

import java.util.List;

public interface GetTenantUseCase {
    TenantResponse getTenantById(Long id);
    TenantResponse getTenantBySlug(String slug);
    List<TenantResponse> getAllTenants();
}
