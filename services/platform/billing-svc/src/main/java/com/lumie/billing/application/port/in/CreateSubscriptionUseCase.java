package com.lumie.billing.application.port.in;

import com.lumie.billing.application.dto.request.CreateSubscriptionRequest;
import com.lumie.billing.application.dto.response.SubscriptionResponse;

public interface CreateSubscriptionUseCase {
    SubscriptionResponse createSubscription(CreateSubscriptionRequest request);
    SubscriptionResponse createFreeSubscription(Long tenantId, String tenantSlug);
}
