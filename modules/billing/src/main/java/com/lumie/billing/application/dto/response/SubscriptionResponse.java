package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.vo.SubscriptionStatus;
import com.lumie.billing.domain.vo.SuspensionReason;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
public record SubscriptionResponse(
        Long id,
        Long tenantId,
        String tenantSlug,
        String planId,
        String planName,
        SubscriptionStatus status,
        @Schema(nullable = true) SuspensionReason suspensionReason,
        @Schema(nullable = true) Long billingKeyId,
        Instant startedAt,
        @Schema(nullable = true) Instant nextChargeAt,
        @Schema(nullable = true) Instant expiresAt,
        @Schema(nullable = true) Instant cancelledAt,
        PlanLimitsResponse limits,
        Instant createdAt,
        Instant updatedAt
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .tenantId(subscription.getTenantId())
                .tenantSlug(subscription.getTenantSlug())
                .planId(subscription.getPlanId())
                .planName(subscription.getPlanName())
                .status(subscription.getStatus())
                .suspensionReason(subscription.getSuspensionReason())
                .billingKeyId(subscription.getBillingKeyId())
                .startedAt(subscription.getStartedAt())
                .nextChargeAt(subscription.getNextChargeAt())
                .expiresAt(subscription.getExpiresAt())
                .cancelledAt(subscription.getCancelledAt())
                .limits(PlanLimitsResponse.from(subscription.getLimits()))
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }
}
