package com.lumie.exam.adapter.out.messaging;

import com.lumie.exam.application.dto.message.OmrGradingImageMessage;
import com.lumie.exam.application.dto.message.ReportGenerationMessage;
import com.lumie.exam.application.event.OmrGradingRequestedEvent;
import com.lumie.exam.application.event.ReportGenerationRequestedEvent;
import com.lumie.messaging.config.RabbitMqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Transactional-outbox bridge: drains grading/report request events from the
 * Spring Modulith {@code event_publication} table to RabbitMQ.
 *
 * <p>Each event was written to the outbox in the same DB transaction as its job
 * row, so "job persisted" and "message will be sent" are atomic. {@link ApplicationModuleListener}
 * dispatches after the originating commit; if the broker send throws, the
 * publication stays incomplete and is retried on application restart
 * ({@code spring.modulith.events.republish-outstanding-events-on-restart=true}).
 * Worker-side per-index dedup makes a republished message idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobRequestForwarder {

    private final RabbitTemplate rabbitTemplate;

    @ApplicationModuleListener
    public void onOmrGradingRequested(OmrGradingRequestedEvent event) {
        OmrGradingImageMessage message = new OmrGradingImageMessage(
                event.jobId(), event.examId(), event.tenantSlug(),
                event.imageKey(), event.imageIndex(), event.totalImages(), 1);
        rabbitTemplate.convertAndSend(
                RabbitMqConstants.LUMIE_COMMANDS_EXCHANGE,
                RabbitMqConstants.GRADING_OMR_REQUEST_ROUTING_KEY,
                message);
        log.info("OMR grading message forwarded: jobId={}, imageIndex={}/{}",
                event.jobId(), event.imageIndex() + 1, event.totalImages());
    }

    @ApplicationModuleListener
    public void onReportGenerationRequested(ReportGenerationRequestedEvent event) {
        ReportGenerationMessage message = new ReportGenerationMessage(
                event.jobId(), event.examId(), event.studentId(),
                event.tenantSlug(), event.reportIndex(), event.totalReports(), 1);
        rabbitTemplate.convertAndSend(
                RabbitMqConstants.LUMIE_COMMANDS_EXCHANGE,
                RabbitMqConstants.REPORT_REQUEST_ROUTING_KEY,
                message);
        log.info("Report generation message forwarded: jobId={}, reportIndex={}/{}",
                event.jobId(), event.reportIndex() + 1, event.totalReports());
    }
}
