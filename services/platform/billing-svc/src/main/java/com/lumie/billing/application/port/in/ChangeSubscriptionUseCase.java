package com.lumie.billing.application.port.in;

import com.lumie.billing.application.dto.request.ChangeSubscriptionRequest;
import com.lumie.billing.application.dto.response.SubscriptionResponse;

public interface ChangeSubscriptionUseCase {
    SubscriptionResponse changePlan(String tenantSlug, ChangeSubscriptionRequest request);
}
