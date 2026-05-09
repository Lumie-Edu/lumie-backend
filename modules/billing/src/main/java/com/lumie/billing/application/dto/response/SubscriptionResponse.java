package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.vo.PlanLimits;
import com.lumie.billing.domain.vo.SubscriptionStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SubscriptionResponse(
        Long id,
        Long tenantId,
        String tenantSlug,
        String planId,
        String planName,
        SubscriptionStatus status,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        PlanLimits limits,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .tenantId(subscription.getTenantId())
                .tenantSlug(subscription.getTenantSlug())
                .planId(subscription.getPlanId())
                .planName(subscription.getPlanName())
                .status(subscription.getStatus())
                .startedAt(subscription.getStartedAt())
                .expiresAt(subscription.getExpiresAt())
                .limits(subscription.getLimits())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }
}
