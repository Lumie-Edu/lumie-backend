package com.lumie.internal.billing;

import java.util.List;
import java.util.Optional;

public interface BillingService {

    Optional<SubscriptionData> getSubscription(String tenantSlug);

    QuotaResult checkQuota(String tenantSlug, String metricType);

    Optional<PlanFeaturesData> getPlanFeatures(String planId);

    SubscriptionData createFreeSubscription(Long tenantId, String tenantSlug);

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
            String metricType,
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
            int maxAdmins,
            int omrMonthlyQuota
    ) {}
}
