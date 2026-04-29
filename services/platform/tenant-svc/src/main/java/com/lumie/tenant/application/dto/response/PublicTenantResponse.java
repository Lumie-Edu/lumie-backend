package com.lumie.tenant.application.dto.response;

import com.lumie.tenant.domain.entity.Tenant;
import lombok.Builder;

@Builder
public record PublicTenantResponse(
        String slug,
        String name,
        String customId,
        String logoUrl
) {
    public static PublicTenantResponse from(Tenant tenant) {
        boolean hasLogo = tenant.getLogoObjectKey() != null
                && !tenant.getLogoObjectKey().isBlank();
        return PublicTenantResponse.builder()
                .slug(tenant.getSlugValue())
                .name(tenant.getName())
                .customId(tenant.getCustomId())
                .logoUrl(hasLogo ? "/api/tenant/v1/tenants/" + tenant.getSlugValue() + "/logo" : null)
                .build();
    }
}
