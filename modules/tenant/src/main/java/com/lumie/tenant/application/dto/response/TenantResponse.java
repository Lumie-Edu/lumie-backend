package com.lumie.tenant.application.dto.response;

import com.lumie.tenant.domain.entity.Tenant;
import com.lumie.tenant.domain.vo.TenantPlan;
import com.lumie.tenant.api.TenantStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
public record TenantResponse(
        Long id,
        String slug,
        String name,
        @Schema(nullable = true) String displayName,
        TenantStatus status,
        TenantPlan plan,
        @Schema(nullable = true) String ownerEmail,
        @Schema(nullable = true) String phone,
        @Schema(nullable = true) String address,
        @Schema(nullable = true) String instituteName,
        @Schema(nullable = true) String businessRegistrationNumber,
        @Schema(nullable = true) String logoUrl,
        @Schema(nullable = true) String customId,
        Instant createdAt,
        Instant updatedAt
) {
    public static TenantResponse from(Tenant tenant) {
        boolean hasLogo = tenant.getLogoObjectKey() != null
                && !tenant.getLogoObjectKey().isBlank();
        return TenantResponse.builder()
                .id(tenant.getId())
                .slug(tenant.getSlugValue())
                .name(tenant.getName())
                .displayName(tenant.getDisplayName())
                .status(tenant.getStatus())
                .plan(tenant.getPlan())
                .ownerEmail(tenant.getOwnerEmail())
                .phone(tenant.getPhone())
                .address(tenant.getAddress())
                .instituteName(tenant.getInstituteName())
                .businessRegistrationNumber(tenant.getBusinessRegistrationNumber())
                .logoUrl(hasLogo ? "/api/tenant/v1/tenants/" + tenant.getSlugValue() + "/logo" : null)
                .customId(tenant.getCustomId())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }
}
