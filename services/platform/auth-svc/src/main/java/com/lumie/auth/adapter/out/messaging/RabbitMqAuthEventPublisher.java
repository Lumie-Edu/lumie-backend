package com.lumie.auth.adapter.out.messaging;

import com.lumie.auth.application.port.out.AuthEventPublisherPort;
import com.lumie.messaging.config.RabbitMqConstants;
import com.lumie.messaging.event.UserLoginEvent;
import com.lumie.messaging.event.UserLogoutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMqAuthEventPublisher implements AuthEventPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishLoginEvent(Long userId, String tenantSlug, String email, String provider) {
        UserLoginEvent event = new UserLoginEvent(userId, tenantSlug, email, provider);
        String routingKey = event.getRoutingKey();

        log.info("Publishing UserLoginEvent: userId={} tenant={} provider={}",
                userId, tenantSlug, provider);

        rabbitTemplate.convertAndSend(
                RabbitMqConstants.LUMIE_EVENTS_EXCHANGE,
                routingKey,
                event
        );

        log.debug("UserLoginEvent published successfully: {}", event.getEventId());
    }

    @Override
    public void publishLogoutEvent(Long userId, String tenantSlug, boolean logoutAll) {
        UserLogoutEvent event = new UserLogoutEvent(userId, tenantSlug, logoutAll);
        String routingKey = event.getRoutingKey();

        log.info("Publishing UserLogoutEvent: userId={} tenant={} logoutAll={}",
                userId, tenantSlug, logoutAll);

        rabbitTemplate.convertAndSend(
                RabbitMqConstants.LUMIE_EVENTS_EXCHANGE,
                routingKey,
                event
        );

        log.debug("UserLogoutEvent published successfully: {}", event.getEventId());
    }
}
