package com.lumie.tenant.application.dto.response;

import com.lumie.tenant.domain.entity.Tenant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PublicTenantResponse(
        String slug,
        String name,
        @Schema(nullable = true) String phone,
        @Schema(nullable = true) String address,
        @Schema(nullable = true) String customId,
        @Schema(nullable = true) String logoUrl
) {
    public static PublicTenantResponse from(Tenant tenant) {
        boolean hasLogo = tenant.getLogoObjectKey() != null
                && !tenant.getLogoObjectKey().isBlank();
        return PublicTenantResponse.builder()
                .slug(tenant.getSlugValue())
                .name(tenant.getName())
                .phone(tenant.getPhone())
                .address(tenant.getAddress())
                .customId(tenant.getCustomId())
                .logoUrl(hasLogo ? "/api/v1/tenants/" + tenant.getSlugValue() + "/logo" : null)
                .build();
    }
}
