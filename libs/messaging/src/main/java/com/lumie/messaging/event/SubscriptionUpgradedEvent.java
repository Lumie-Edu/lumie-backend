package com.lumie.messaging.event;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class SubscriptionUpgradedEvent extends BillingEvent {

    private final Long subscriptionId;
    private final String previousPlanId;
    private final String newPlanId;
    private final String newPlanName;

    public SubscriptionUpgradedEvent(Long tenantId, String tenantSlug, Long subscriptionId,
                                     String previousPlanId, String newPlanId, String newPlanName) {
        super(tenantId, tenantSlug);
        this.subscriptionId = subscriptionId;
        this.previousPlanId = previousPlanId;
        this.newPlanId = newPlanId;
        this.newPlanName = newPlanName;
    }

    @Override
    public String getRoutingKey() {
        return String.format("billing.subscription.upgraded.%s", getTenantSlug());
    }
}
