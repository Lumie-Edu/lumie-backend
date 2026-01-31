package com.lumie.academy.adapter.out.messaging;

import com.lumie.academy.application.port.out.MemberEventPublisherPort;
import com.lumie.messaging.config.RabbitMqConstants;
import com.lumie.messaging.event.MemberEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqMemberEventPublisher implements MemberEventPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(MemberEvent event) {
        String routingKey = event.getRoutingKey();
        log.debug("Publishing member event: {} with routing key: {}", event.getEventType(), routingKey);

        rabbitTemplate.convertAndSend(
            RabbitMqConstants.LUMIE_EVENTS_EXCHANGE,
            routingKey,
            event
        );

        log.info("Published member event: {} for member: {}", event.getEventType(), event.getMemberId());
    }
}
