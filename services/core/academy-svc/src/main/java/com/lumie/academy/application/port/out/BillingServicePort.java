package com.lumie.academy.application.port.out;

public interface BillingServicePort {

    QuotaCheckResult checkQuota(String tenantSlug, String metricType);

    record QuotaCheckResult(boolean allowed, long currentUsage, long limit, String message) {
    }
}
