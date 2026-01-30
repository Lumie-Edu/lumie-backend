package com.lumie.billing.application.port.in;

import com.lumie.billing.application.dto.response.QuotaCheckResponse;
import com.lumie.billing.domain.vo.MetricType;

public interface CheckQuotaUseCase {
    QuotaCheckResponse checkQuota(String tenantSlug, MetricType metricType);
    QuotaCheckResponse checkQuota(String tenantSlug, MetricType metricType, long currentUsage);
}
