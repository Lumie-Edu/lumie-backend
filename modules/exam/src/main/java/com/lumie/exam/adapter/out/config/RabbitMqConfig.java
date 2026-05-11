package com.lumie.exam.adapter.out.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * AMQP client-side wiring for the monolith.
 *
 * <p>Exchanges, queues, bindings, and policies are declared as
 * RabbitMQ Topology Operator Custom Resources in the
 * {@code lumie-infra/platform/rabbitmq/base/topology/} directory, so this
 * class only retains the application-layer beans that publishers and
 * consumers depend on: {@link RabbitTemplate} (with JSON converter),
 * {@link MessageConverter}, and a listener-container factory tuned for
 * the broker-side delivery-limit policy.
 */
@Configuration
public class RabbitMqConfig {

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

    /**
     * Spring AMQP default is {@code defaultRequeueRejected=true}, which
     * tight-loops a failing message back into the same listener thread.
     * We rely on the quorum queue's broker-side {@code delivery-limit: 5}
     * policy (declared in {@code lumie-infra/.../topology/policies.yaml})
     * to route failed messages to DLQ — so let the broker count
     * redeliveries by rejecting without requeue. After 5 deliveries the
     * message hits {@code lumie.dlx} and lands in the matching
     * {@code grading.# / report.#} DLQ for operator replay.
     */
    @Bean
    @Primary
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
