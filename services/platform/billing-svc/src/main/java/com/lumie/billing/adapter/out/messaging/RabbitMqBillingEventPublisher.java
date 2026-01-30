package com.lumie.billing.adapter.out.messaging;

import com.lumie.billing.application.port.out.BillingEventPublisherPort;
import com.lumie.messaging.config.RabbitMqConstants;
import com.lumie.messaging.event.BillingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqBillingEventPublisher implements BillingEventPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(BillingEvent event) {
        String routingKey = event.getRoutingKey();
        log.info("Publishing billing event: {} with routing key: {}",
                event.getEventType(), routingKey);

        rabbitTemplate.convertAndSend(
                RabbitMqConstants.LUMIE_EVENTS_EXCHANGE,
                routingKey,
                event
        );

        log.debug("Billing event published successfully: eventId={}", event.getEventId());
    }
}
