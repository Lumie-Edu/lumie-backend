package com.lumie.messaging.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class QuotaExceededEvent extends BillingEvent {

    private final String metricType;
    private final long currentUsage;
    private final long limit;

    public QuotaExceededEvent(Long tenantId, String tenantSlug, String metricType,
                              long currentUsage, long limit) {
        super(tenantId, tenantSlug);
        this.metricType = metricType;
        this.currentUsage = currentUsage;
        this.limit = limit;
    }

    @Override
    public String getRoutingKey() {
        return String.format("billing.quota.exceeded.%s", getTenantSlug());
    }
}
