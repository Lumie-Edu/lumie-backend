package com.lumie.common.exception;

import com.lumie.common.quota.MetricType;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thrown when a tenant's usage on a quota-bounded metric would exceed the plan limit.
 * <p>
 * Tier-1 typed business exception ({@link BusinessException} subclass): the four typed
 * fields below survive into {@code ProblemDetail.properties} via
 * {@code GlobalExceptionHandler}, so REST clients can render upsell CTAs without
 * parsing the message string.
 * <p>
 * The {@link ErrorCode} is supplied by the throwing module (e.g.
 * {@code BillingErrorCode.QUOTA_EXCEEDED}, {@code StaffErrorCode.QUOTA_EXCEEDED},
 * {@code StudentErrorCode.QUOTA_EXCEEDED}) so the module-scoped code prefix is
 * preserved in error responses.
 */
@Getter
public class QuotaExceededException extends BusinessException implements ExtensionProperties {

    private final String tenantSlug;
    private final MetricType metricType;
    private final long currentUsage;
    private final long limit;

    public QuotaExceededException(ErrorCode errorCode,
                                  String tenantSlug,
                                  MetricType metricType,
                                  long currentUsage,
                                  long limit) {
        super(errorCode, formatMessage(tenantSlug, metricType, currentUsage, limit));
        this.tenantSlug = tenantSlug;
        this.metricType = metricType;
        this.currentUsage = currentUsage;
        this.limit = limit;
    }

    private static String formatMessage(String tenantSlug,
                                        MetricType metricType,
                                        long currentUsage,
                                        long limit) {
        return String.format("Quota exceeded for tenant '%s'. %s: %d/%d",
                tenantSlug, metricType, currentUsage, limit);
    }

    @Override
    public Map<String, Object> extensionProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("tenantSlug", tenantSlug);
        properties.put("metricType", metricType);
        properties.put("currentUsage", currentUsage);
        properties.put("limit", limit);
        return properties;
    }
}
