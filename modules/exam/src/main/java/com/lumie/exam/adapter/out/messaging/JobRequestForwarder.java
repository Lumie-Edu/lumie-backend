package com.lumie.exam.adapter.out.messaging;

import com.lumie.exam.application.dto.message.OmrGradingImageMessage;
import com.lumie.exam.application.dto.message.ReportBatchGenerationMessage;
import com.lumie.exam.application.dto.message.ReportGenerationMessage;
import com.lumie.exam.application.event.OmrGradingRequestedEvent;
import com.lumie.exam.application.event.ReportBatchGenerationRequestedEvent;
import com.lumie.exam.application.event.ReportGenerationRequestedEvent;
import com.lumie.messaging.config.RabbitMqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import static com.lumie.common.logging.LogFields.field;

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
                event.imageIndex(), event.totalImages(), 2);
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConstants.LUMIE_COMMANDS_EXCHANGE,
                    RabbitMqConstants.GRADING_OMR_REQUEST_ROUTING_KEY,
                    message);
            log.info("Background job enqueued omr_grading jobId={} imageIndex={}/{}",
                    event.jobId(), event.imageIndex() + 1, event.totalImages(),
                    field("eventType", "background_job"),
                    field("jobName", "omr_grading"),
                    field("jobPhase", "enqueued"),
                    field("jobId", event.jobId()),
                    field("tenantSlug", event.tenantSlug()),
                    field("entityType", "exam"),
                    field("entityId", event.examId()),
                    field("queue", "grading.omr-request"),
                    field("routingKey", RabbitMqConstants.GRADING_OMR_REQUEST_ROUTING_KEY),
                    field("itemIndex", event.imageIndex()),
                    field("totalItems", event.totalImages()),
                    field("outcome", "success"));
        } catch (RuntimeException e) {
            log.error("Background job enqueue failed omr_grading jobId={}",
                    event.jobId(),
                    field("eventType", "background_job"),
                    field("jobName", "omr_grading"),
                    field("jobPhase", "enqueue_failed"),
                    field("jobId", event.jobId()),
                    field("tenantSlug", event.tenantSlug()),
                    field("entityType", "exam"),
                    field("entityId", event.examId()),
                    field("queue", "grading.omr-request"),
                    field("routingKey", RabbitMqConstants.GRADING_OMR_REQUEST_ROUTING_KEY),
                    field("itemIndex", event.imageIndex()),
                    field("totalItems", event.totalImages()),
                    field("outcome", "failed"),
                    field("exceptionClass", e.getClass().getSimpleName()),
                    e);
            throw e;
        }
    }

    @ApplicationModuleListener
    public void onReportGenerationRequested(ReportGenerationRequestedEvent event) {
        ReportGenerationMessage message = new ReportGenerationMessage(
                event.jobId(), event.examId(), event.studentId(),
                event.tenantSlug(), event.reportIndex(), event.totalReports(), 1);
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConstants.LUMIE_COMMANDS_EXCHANGE,
                    RabbitMqConstants.REPORT_REQUEST_ROUTING_KEY,
                    message);
            log.info("Background job enqueued report_generation jobId={} reportIndex={}/{}",
                    event.jobId(), event.reportIndex() + 1, event.totalReports(),
                    field("eventType", "background_job"),
                    field("jobName", "report_generation"),
                    field("jobPhase", "enqueued"),
                    field("jobId", event.jobId()),
                    field("tenantSlug", event.tenantSlug()),
                    field("entityType", "exam"),
                    field("entityId", event.examId()),
                    field("queue", RabbitMqConstants.REPORT_REQUEST_QUEUE),
                    field("routingKey", RabbitMqConstants.REPORT_REQUEST_ROUTING_KEY),
                    field("itemIndex", event.reportIndex()),
                    field("totalItems", event.totalReports()),
                    field("outcome", "success"));
        } catch (RuntimeException e) {
            log.error("Background job enqueue failed report_generation jobId={}",
                    event.jobId(),
                    field("eventType", "background_job"),
                    field("jobName", "report_generation"),
                    field("jobPhase", "enqueue_failed"),
                    field("jobId", event.jobId()),
                    field("tenantSlug", event.tenantSlug()),
                    field("entityType", "exam"),
                    field("entityId", event.examId()),
                    field("queue", RabbitMqConstants.REPORT_REQUEST_QUEUE),
                    field("routingKey", RabbitMqConstants.REPORT_REQUEST_ROUTING_KEY),
                    field("itemIndex", event.reportIndex()),
                    field("totalItems", event.totalReports()),
                    field("outcome", "failed"),
                    field("exceptionClass", e.getClass().getSimpleName()),
                    e);
            throw e;
        }
    }

    @ApplicationModuleListener
    public void onReportBatchGenerationRequested(ReportBatchGenerationRequestedEvent event) {
        ReportBatchGenerationMessage message = new ReportBatchGenerationMessage(
                event.jobId(), event.examId(), event.studentIds(),
                event.tenantSlug(), event.startIndex(), event.totalReports(), 2);
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConstants.LUMIE_COMMANDS_EXCHANGE,
                    RabbitMqConstants.REPORT_REQUEST_ROUTING_KEY,
                    message);
            log.info("Background job enqueued report_generation batch jobId={} startIndex={} size={} total={}",
                    event.jobId(), event.startIndex(), event.studentIds().size(), event.totalReports(),
                    field("eventType", "background_job"),
                    field("jobName", "report_generation"),
                    field("jobPhase", "enqueued"),
                    field("jobId", event.jobId()),
                    field("tenantSlug", event.tenantSlug()),
                    field("entityType", "exam"),
                    field("entityId", event.examId()),
                    field("queue", RabbitMqConstants.REPORT_REQUEST_QUEUE),
                    field("routingKey", RabbitMqConstants.REPORT_REQUEST_ROUTING_KEY),
                    field("itemIndex", event.startIndex()),
                    field("totalItems", event.totalReports()),
                    field("schemaVersion", 2),
                    field("outcome", "success"));
        } catch (RuntimeException e) {
            log.error("Background job enqueue failed report_generation batch jobId={}",
                    event.jobId(),
                    field("eventType", "background_job"),
                    field("jobName", "report_generation"),
                    field("jobPhase", "enqueue_failed"),
                    field("jobId", event.jobId()),
                    field("tenantSlug", event.tenantSlug()),
                    field("entityType", "exam"),
                    field("entityId", event.examId()),
                    field("queue", RabbitMqConstants.REPORT_REQUEST_QUEUE),
                    field("routingKey", RabbitMqConstants.REPORT_REQUEST_ROUTING_KEY),
                    field("itemIndex", event.startIndex()),
                    field("totalItems", event.totalReports()),
                    field("schemaVersion", 2),
                    field("outcome", "failed"),
                    field("exceptionClass", e.getClass().getSimpleName()),
                    e);
            throw e;
        }
    }
}
