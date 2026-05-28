package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.entity.BillingKey;
import com.lumie.billing.domain.vo.BillingKeyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record BillingKeyResponse(
        Long id,
        Long tenantId,
        @Schema(nullable = true, description = "마스킹된 카드 번호 (Toss가 미반환 시 null)")
        String maskedCardNumber,
        @Schema(nullable = true, description = "카드사 (Toss가 미반환 시 null)")
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
