package com.lumie.tenant.application.dto.response;

import com.lumie.tenant.domain.entity.Tenant;
import com.lumie.tenant.domain.vo.TenantPlan;
import com.lumie.tenant.api.TenantStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TenantResponse(
        Long id,
        String slug,
        String name,
        String displayName,
        TenantStatus status,
        TenantPlan plan,
        String schemaName,
        String ownerEmail,
        String phone,
        String address,
        String instituteName,
        String businessRegistrationNumber,
        String logoUrl,
        String customId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
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
                .schemaName(tenant.getSchemaName())
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
