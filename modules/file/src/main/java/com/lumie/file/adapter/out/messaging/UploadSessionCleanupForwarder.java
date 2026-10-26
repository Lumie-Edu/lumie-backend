package com.lumie.file.adapter.out.messaging;

import com.lumie.file.application.dto.message.UploadSessionCleanupMessage;
import com.lumie.file.application.event.UploadSessionCleanupDueEvent;
import com.lumie.file.application.event.UploadSessionCleanupRetryEvent;
import com.lumie.messaging.config.RabbitMqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class UploadSessionCleanupForwarder {
    private final RabbitTemplate rabbitTemplate;

    @ApplicationModuleListener
    public void onCleanupDue(UploadSessionCleanupDueEvent event) {
        publishConfirmed(RabbitMqConstants.FILE_UPLOAD_CLEANUP_DELAY_ROUTING_KEY,
                new UploadSessionCleanupMessage(event.sessionId(), event.tenantId(), event.tenantSlug(),
                        event.cleanupNotBefore(), 0));
        log.info("Upload session cleanup scheduled sessionId={} cleanupNotBefore={}",
                event.sessionId(), event.cleanupNotBefore());
    }

    @ApplicationModuleListener
    public void onCleanupRetry(UploadSessionCleanupRetryEvent event) {
        if (event.attempt() >= 5) {
            publishConfirmed("file.upload-session.cleanup.terminal",
                    new UploadSessionCleanupMessage(event.sessionId(), event.tenantId(), event.tenantSlug(),
                            event.cleanupNotBefore(), event.attempt()));
            return;
        }
        publishConfirmed(
                "file.upload-session-cleanup.retry",
                new UploadSessionCleanupMessage(event.sessionId(), event.tenantId(), event.tenantSlug(),
                        event.cleanupNotBefore(), event.attempt()));
    }

    private void publishConfirmed(String routingKey, UploadSessionCleanupMessage message) {
        CorrelationData correlation = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(RabbitMqConstants.LUMIE_COMMANDS_EXCHANGE, routingKey, message, correlation);
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlation.getReturned() != null) {
                throw new IllegalStateException("RabbitMQ did not route upload cleanup message");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while awaiting RabbitMQ publisher confirm", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to publish durable upload cleanup message", exception);
        }
    }
}
