package com.lumie.billing.api;

import com.lumie.common.quota.MetricType;

import java.util.List;
import java.util.Optional;

public interface BillingService {

    Optional<SubscriptionData> getSubscription(String tenantSlug);

    QuotaResult checkQuota(String tenantSlug, MetricType metricType);

    Optional<PlanFeaturesData> getPlanFeatures(String planId);

    SubscriptionData createFreeSubscription(Long tenantId, String tenantSlug);

    /**
     * Creates the signup trial in the caller's transaction and tenant context.
     * Implementations must propagate failures so tenant signup can roll back.
     */
    SubscriptionData createTrialSubscription(Long tenantId, String tenantSlug);

    record SubscriptionData(
            Long id,
            Long tenantId,
            String tenantSlug,
            String planId,
            String planName,
            String status,
            String startedAt,
            String expiresAt,
            PlanLimits limits
    ) {}

    record QuotaResult(
            boolean allowed,
            long currentUsage,
            long limit,
            MetricType metricType,
            String message
    ) {}

    record PlanFeaturesData(
            String planId,
            String planName,
            PlanLimits limits,
            List<String> features
    ) {}

    record PlanLimits(
            int maxStudents,
            int maxAcademies,
            int maxStaff,
            int omrMonthlyQuota
    ) {}
}
