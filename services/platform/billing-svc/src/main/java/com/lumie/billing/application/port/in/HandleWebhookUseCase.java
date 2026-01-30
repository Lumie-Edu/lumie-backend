package com.lumie.billing.application.port.in;

import com.lumie.billing.application.dto.request.TossWebhookPayload;

public interface HandleWebhookUseCase {
    void handleWebhook(TossWebhookPayload payload);
}
