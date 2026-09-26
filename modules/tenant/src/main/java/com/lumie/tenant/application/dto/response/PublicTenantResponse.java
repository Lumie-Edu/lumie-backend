package com.lumie.tenant.application.dto.response;

import com.lumie.tenant.domain.entity.Tenant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PublicTenantResponse(
        String slug,
        String name,
        @Schema(nullable = true) String phone,
        @Schema(nullable = true) String logoUrl,
        @Schema(allowableValues = {"default", "ocean"}) String loginTheme
) {
    public static PublicTenantResponse from(Tenant tenant, String loginTheme) {
        boolean hasLogo = tenant.getLogoObjectKey() != null
                && !tenant.getLogoObjectKey().isBlank();
        return PublicTenantResponse.builder()
                .slug(tenant.getSlugValue())
                .name(tenant.getName())
                .phone(tenant.getPhone())
                .logoUrl(hasLogo ? "/api/v1/tenants/" + tenant.getSlugValue() + "/logo" : null)
                .loginTheme(loginTheme)
                .build();
    }
}
