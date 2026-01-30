package com.lumie.messaging.event;

import lombok.Getter;

@Getter
public class SubscriptionCreatedEvent extends BillingEvent {

    private final Long subscriptionId;
    private final String planId;
    private final String planName;

    public SubscriptionCreatedEvent(Long tenantId, String tenantSlug, Long subscriptionId,
                                    String planId, String planName) {
        super(tenantId, tenantSlug);
        this.subscriptionId = subscriptionId;
        this.planId = planId;
        this.planName = planName;
    }

    @Override
    public String getRoutingKey() {
        return String.format("billing.subscription.created.%s", getTenantSlug());
    }
}
