package com.lumie.billing.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lumie.messaging.config.RabbitMqConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange lumieEventsExchange() {
        return new TopicExchange(RabbitMqConstants.LUMIE_EVENTS_EXCHANGE);
    }

    @Bean
    public TopicExchange lumieDlxExchange() {
        return new TopicExchange(RabbitMqConstants.LUMIE_DLX_EXCHANGE);
    }

    @Bean
    public Queue billingSubscriptionQueue() {
        return QueueBuilder.durable(RabbitMqConstants.BILLING_SUBSCRIPTION_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.LUMIE_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.BILLING_SUBSCRIPTION_DLQ)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public Queue billingSubscriptionDlq() {
        return QueueBuilder.durable(RabbitMqConstants.BILLING_SUBSCRIPTION_DLQ).build();
    }

    @Bean
    public Queue billingQuotaQueue() {
        return QueueBuilder.durable(RabbitMqConstants.BILLING_QUOTA_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.LUMIE_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.BILLING_QUOTA_DLQ)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public Queue billingQuotaDlq() {
        return QueueBuilder.durable(RabbitMqConstants.BILLING_QUOTA_DLQ).build();
    }

    @Bean
    public Binding billingSubscriptionBinding() {
        return BindingBuilder
                .bind(billingSubscriptionQueue())
                .to(lumieEventsExchange())
                .with(RabbitMqConstants.TENANT_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding billingSubscriptionDlqBinding() {
        return BindingBuilder
                .bind(billingSubscriptionDlq())
                .to(lumieDlxExchange())
                .with(RabbitMqConstants.BILLING_SUBSCRIPTION_DLQ);
    }

    @Bean
    public Binding billingQuotaBinding() {
        return BindingBuilder
                .bind(billingQuotaQueue())
                .to(lumieEventsExchange())
                .with(RabbitMqConstants.BILLING_QUOTA_EXCEEDED_ROUTING_KEY);
    }

    @Bean
    public Binding billingQuotaDlqBinding() {
        return BindingBuilder
                .bind(billingQuotaDlq())
                .to(lumieDlxExchange())
                .with(RabbitMqConstants.BILLING_QUOTA_DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
