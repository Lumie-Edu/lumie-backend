package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.entity.AlimtalkCredit;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record AlimtalkCreditResponse(
        Long id,
        Long tenantId,
        int balance,
        boolean autoRechargeEnabled,
        @Schema(nullable = true) Integer autoRechargeThreshold,
        @Schema(nullable = true) Integer autoRechargePackage,
        Instant updatedAt
) {
    public static AlimtalkCreditResponse from(AlimtalkCredit credit) {
        return new AlimtalkCreditResponse(
                credit.getId(),
                credit.getTenantId(),
                credit.getBalance(),
                credit.isAutoRechargeEnabled(),
                credit.getAutoRechargeThreshold(),
                credit.getAutoRechargePackage(),
                credit.getUpdatedAt()
        );
    }
}
