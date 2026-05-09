package com.lumie.billing.application.port.out;

public interface WebhookIdempotencyPort {
    boolean isProcessed(String eventId);
    void markProcessed(String eventId, String eventType);
}
