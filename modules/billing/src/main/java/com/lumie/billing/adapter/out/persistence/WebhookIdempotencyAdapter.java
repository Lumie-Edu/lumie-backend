package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.application.port.out.WebhookIdempotencyPort;
import com.lumie.billing.domain.entity.PaddleWebhookEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WebhookIdempotencyAdapter implements WebhookIdempotencyPort {

    private final PaddleWebhookEventJpaRepository repository;

    @Override
    public boolean isProcessed(String eventId) {
        return repository.existsById(eventId);
    }

    @Override
    public void markProcessed(String eventId, String eventType) {
        repository.save(new PaddleWebhookEvent(eventId, eventType));
    }
}
