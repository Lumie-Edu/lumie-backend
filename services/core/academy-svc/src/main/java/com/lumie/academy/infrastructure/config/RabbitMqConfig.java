package com.lumie.academy.infrastructure.config;

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
    public Queue memberEventsQueue() {
        return QueueBuilder.durable(MEMBER_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", LUMIE_DLX_EXCHANGE)
                .withArgument("x-message-ttl", 300000)
                .build();
    }

    @Bean
    public Queue memberEventsDlq() {
        return QueueBuilder.durable(MEMBER_EVENTS_DLQ)
                .build();
    }

    @Bean
    public Binding memberCreatedBinding() {
        return BindingBuilder
                .bind(memberEventsQueue())
                .to(lumieEventsExchange())
                .with(MEMBER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding memberUpdatedBinding() {
        return BindingBuilder
                .bind(memberEventsQueue())
                .to(lumieEventsExchange())
                .with(MEMBER_UPDATED_ROUTING_KEY);
    }

    @Bean
    public Binding memberDeletedBinding() {
        return BindingBuilder
                .bind(memberEventsQueue())
                .to(lumieEventsExchange())
                .with(MEMBER_DELETED_ROUTING_KEY);
    }

    @Bean
    public Binding memberEventsDlqBinding() {
        return BindingBuilder
                .bind(memberEventsDlq())
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
