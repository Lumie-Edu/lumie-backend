package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.vo.MetricType;
import lombok.Builder;

@Builder
public record QuotaCheckResponse(
        boolean allowed,
        long currentUsage,
        long limit,
        MetricType metricType,
        String message
) {
    public static QuotaCheckResponse allowed(MetricType metricType, long currentUsage, long limit) {
        return QuotaCheckResponse.builder()
                .allowed(true)
                .currentUsage(currentUsage)
                .limit(limit)
                .metricType(metricType)
                .message("Quota available")
                .build();
    }

    public static QuotaCheckResponse exceeded(MetricType metricType, long currentUsage, long limit) {
        return QuotaCheckResponse.builder()
                .allowed(false)
                .currentUsage(currentUsage)
                .limit(limit)
                .metricType(metricType)
                .message(String.format("Quota exceeded for %s: %d/%d", metricType, currentUsage, limit))
                .build();
    }
}
