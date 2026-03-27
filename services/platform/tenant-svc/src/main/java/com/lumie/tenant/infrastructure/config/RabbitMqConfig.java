package com.lumie.tenant.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.lumie.messaging.config.RabbitMqConstants.*;

@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange lumieEventsExchange() {
        return new TopicExchange(LUMIE_EVENTS_EXCHANGE);
    }

    @Bean
    public FanoutExchange lumieDlxExchange() {
        return new FanoutExchange(LUMIE_DLX_EXCHANGE);
    }

    @Bean
    public Queue tenantSchemaProvisioningQueue() {
        return QueueBuilder.durable(TENANT_SCHEMA_PROVISIONING_QUEUE)
                .withArgument("x-queue-type", "quorum")
                .withArgument("x-dead-letter-exchange", LUMIE_DLX_EXCHANGE)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public Queue tenantSchemaProvisioningDlq() {
        return QueueBuilder.durable(TENANT_SCHEMA_PROVISIONING_DLQ)
                .withArgument("x-queue-type", "quorum")
                .build();
    }

    @Bean
    public Binding tenantSchemaProvisioningBinding() {
        return BindingBuilder
                .bind(tenantSchemaProvisioningQueue())
                .to(lumieEventsExchange())
                .with(TENANT_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding tenantSchemaProvisioningDlqBinding() {
        return BindingBuilder
                .bind(tenantSchemaProvisioningDlq())
                .to(lumieDlxExchange());
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
