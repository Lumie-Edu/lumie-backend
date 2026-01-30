package com.lumie.billing.domain.exception;

import com.lumie.billing.domain.vo.MetricType;
import lombok.Getter;

@Getter
public class QuotaExceededException extends RuntimeException {

    private final String tenantSlug;
    private final MetricType metricType;
    private final long currentUsage;
    private final long limit;

    public QuotaExceededException(String tenantSlug, MetricType metricType,
                                   long currentUsage, long limit) {
        super(String.format("Quota exceeded for tenant '%s'. %s: %d/%d",
                tenantSlug, metricType, currentUsage, limit));
        this.tenantSlug = tenantSlug;
        this.metricType = metricType;
        this.currentUsage = currentUsage;
        this.limit = limit;
    }
}
