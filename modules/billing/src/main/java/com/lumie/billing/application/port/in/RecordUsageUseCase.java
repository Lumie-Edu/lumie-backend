package com.lumie.billing.application.port.in;

import com.lumie.billing.domain.vo.MetricType;

public interface RecordUsageUseCase {
    void recordUsage(Long tenantId, String tenantSlug, MetricType metricType, long value);
    void incrementUsage(Long tenantId, String tenantSlug, MetricType metricType, long delta);
}
