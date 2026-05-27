package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.entity.BillingKey;
import com.lumie.billing.domain.vo.BillingKeyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record BillingKeyResponse(
        Long id,
        Long tenantId,
        String maskedCardNumber,
        String cardCompany,
        BillingKeyStatus status,
        @Schema(nullable = true) Instant revokedAt,
        Instant createdAt
) {
    public static BillingKeyResponse from(BillingKey billingKey) {
        return new BillingKeyResponse(
                billingKey.getId(),
                billingKey.getTenantId(),
                billingKey.getMaskedCardNumber(),
                billingKey.getCardCompany(),
                billingKey.getStatus(),
                billingKey.getRevokedAt(),
                billingKey.getCreatedAt()
        );
    }
}
