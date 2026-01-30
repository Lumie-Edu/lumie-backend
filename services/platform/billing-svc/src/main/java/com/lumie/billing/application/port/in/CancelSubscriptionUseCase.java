package com.lumie.billing.application.port.in;

public interface CancelSubscriptionUseCase {
    void cancelSubscription(String tenantSlug);
}
