package com.lumie.billing.application.port.in;

import com.lumie.billing.application.dto.response.SubscriptionResponse;

public interface GetSubscriptionUseCase {
    SubscriptionResponse getSubscription(String tenantSlug);
    SubscriptionResponse getSubscriptionByTenantId(Long tenantId);
}
