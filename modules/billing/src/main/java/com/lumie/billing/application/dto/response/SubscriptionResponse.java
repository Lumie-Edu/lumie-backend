package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.vo.PlanLimits;
import com.lumie.billing.domain.vo.SubscriptionStatus;
import lombok.Builder;

import java.time.Instant;
import com.lumie.common.util.Timestamps;

@Builder
public record SubscriptionResponse(
        Long id,
        Long tenantId,
        String tenantSlug,
        String planId,
        String planName,
        SubscriptionStatus status,
        Instant startedAt,
        Instant expiresAt,
        PlanLimits limits,
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
                .startedAt(Timestamps.utcInstant(subscription.getStartedAt()))
                .expiresAt(Timestamps.utcInstant(subscription.getExpiresAt()))
                .limits(subscription.getLimits())
                .createdAt(Timestamps.utcInstant(subscription.getCreatedAt()))
                .updatedAt(Timestamps.utcInstant(subscription.getUpdatedAt()))
                .build();
    }
}
