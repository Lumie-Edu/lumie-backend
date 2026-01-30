package com.lumie.billing.adapter.in.messaging;

import com.lumie.billing.application.port.in.CreateSubscriptionUseCase;
import com.lumie.messaging.config.RabbitMqConstants;
import com.lumie.messaging.event.TenantCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantEventListener {

    private final CreateSubscriptionUseCase createSubscriptionUseCase;

    @RabbitListener(queues = RabbitMqConstants.BILLING_SUBSCRIPTION_QUEUE)
    public void handleTenantCreated(TenantCreatedEvent event) {
        log.info("Received TenantCreatedEvent: tenantId={}, slug={}",
                event.getTenantId(), event.getSlug());

        try {
            createSubscriptionUseCase.createFreeSubscription(
                    event.getTenantId(),
                    event.getSlug()
            );
            log.info("Free subscription created for tenant: {}", event.getSlug());
        } catch (Exception e) {
            log.error("Failed to create free subscription for tenant: {}", event.getSlug(), e);
            throw e;
        }
    }
}
