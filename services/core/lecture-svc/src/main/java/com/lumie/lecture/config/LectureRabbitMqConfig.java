package com.lumie.lecture.config;

import com.lumie.messaging.config.RabbitMqConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LectureRabbitMqConfig {

    @Bean
    public Queue lectureProcessQueue() {
        return QueueBuilder.durable(RabbitMqConstants.LECTURE_PROCESS_QUEUE)
                .withArgument("x-queue-type", "quorum")
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.LUMIE_DLX_EXCHANGE)
                .withArgument("x-message-ttl", 1800000) // 30 minutes
                .build();
    }

    @Bean
    public Queue lectureProcessDlq() {
        return QueueBuilder.durable(RabbitMqConstants.LECTURE_PROCESS_DLQ)
                .withArgument("x-queue-type", "quorum")
                .build();
    }

    @Bean
    public Binding lectureProcessBinding(DirectExchange lumieCommandsExchange) {
        return BindingBuilder
                .bind(lectureProcessQueue())
                .to(lumieCommandsExchange)
                .with(RabbitMqConstants.LECTURE_PROCESS_ROUTING_KEY);
    }

    @Bean
    public Binding lectureProcessDlqBinding(FanoutExchange lumieDlxExchange) {
        return BindingBuilder
                .bind(lectureProcessDlq())
                .to(lumieDlxExchange);
    }
}
