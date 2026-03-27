package com.lumie.exam.infrastructure.config;

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
    public DirectExchange lumieCommandsExchange() {
        return new DirectExchange(RabbitMqConstants.LUMIE_COMMANDS_EXCHANGE);
    }

    @Bean
    public FanoutExchange lumieDlxExchange() {
        return new FanoutExchange(RabbitMqConstants.LUMIE_DLX_EXCHANGE);
    }

    @Bean
    public Queue gradingOmrRequestQueue() {
        return QueueBuilder.durable(RabbitMqConstants.GRADING_OMR_REQUEST_QUEUE)
                .withArgument("x-queue-type", "quorum")
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.LUMIE_DLX_EXCHANGE)
                .withArgument("x-message-ttl", 600000)
                .build();
    }

    @Bean
    public Queue gradingOmrRequestDlq() {
        return QueueBuilder.durable(RabbitMqConstants.GRADING_OMR_REQUEST_DLQ)
                .withArgument("x-queue-type", "quorum")
                .build();
    }

    @Bean
    public Binding gradingOmrRequestBinding() {
        return BindingBuilder
                .bind(gradingOmrRequestQueue())
                .to(lumieCommandsExchange())
                .with(RabbitMqConstants.GRADING_OMR_REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding gradingOmrRequestDlqBinding() {
        return BindingBuilder
                .bind(gradingOmrRequestDlq())
                .to(lumieDlxExchange());
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
