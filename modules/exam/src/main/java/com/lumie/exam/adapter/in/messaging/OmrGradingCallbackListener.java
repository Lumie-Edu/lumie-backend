package com.lumie.exam.adapter.in.messaging;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.request.OmrGradingCallbackRequest;
import com.lumie.exam.application.service.OmrGradingJobProcessor;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.tenant.api.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import com.lumie.messaging.config.RabbitMqConstants;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static com.lumie.common.logging.LogFields.field;

/**
 * Consumes grading-svc completion callbacks from RabbitMQ.
 *
 * <p>Replaces the previous {@code @PostMapping("/internal/omr/callback")}
 * HTTP handler. Symmetric with the inbound request side
 * ({@code grading.omr-request}): if {@link OmrGradingJobProcessor}
 * throws, the message hits the broker delivery-limit policy and is
 * routed to the {@code grading.#} DLQ for operator replay — no more
 * silent callback loss when the BE pod restarts mid-callback or the
 * handler raises.
 *
 * <p>Tenant context is propagated via the {@code tenantSlug} field in
 * the JSON payload (same wire shape as the old HTTP body) and bound
 * into {@link TenantContextHolder} for the duration of the call so
 * downstream tenant-aware repositories work unchanged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OmrGradingCallbackListener {

    private final OmrGradingJobProcessor processor;
    private final TenantService tenantService;

    @RabbitListener(queues = RabbitMqConstants.GRADING_OMR_CALLBACK_QUEUE)
    public void handle(OmrGradingCallbackRequest request) {
        long startNanos = System.nanoTime();
        if (request.tenantSlug() == null || request.tenantSlug().isBlank()) {
            log.error("Background job callback rejected omr_grading jobId={} missing tenantSlug",
                    request.jobId(),
                    field("eventType", "background_job"),
                    field("jobName", "omr_grading"),
                    field("jobPhase", "callback_rejected"),
                    field("jobId", request.jobId()),
                    field("entityType", "exam"),
                    field("entityId", request.examId()),
                    field("queue", RabbitMqConstants.GRADING_OMR_CALLBACK_QUEUE),
                    field("latencyMs", elapsedMillis(startNanos)),
                    field("outcome", "failed"),
                    field("errorCode", ExamErrorCode.MISSING_TENANT_SLUG.name()));
            throw new BusinessException(
                    ExamErrorCode.MISSING_TENANT_SLUG,
                    "OMR callback missing tenantSlug for job " + request.jobId()
            );
        }
        Long tenantId = tenantService.getTenantBySlug(request.tenantSlug())
                .map(TenantService.TenantData::id)
                .orElse(null);
        if (tenantId == null) {
            log.error("Background job callback rejected omr_grading jobId={} unknown tenant",
                    request.jobId(),
                    field("eventType", "background_job"),
                    field("jobName", "omr_grading"),
                    field("jobPhase", "callback_rejected"),
                    field("jobId", request.jobId()),
                    field("tenantSlug", request.tenantSlug()),
                    field("entityType", "exam"),
                    field("entityId", request.examId()),
                    field("queue", RabbitMqConstants.GRADING_OMR_CALLBACK_QUEUE),
                    field("latencyMs", elapsedMillis(startNanos)),
                    field("outcome", "failed"),
                    field("errorCode", ExamErrorCode.MISSING_TENANT_SLUG.name()));
            throw new BusinessException(
                    ExamErrorCode.MISSING_TENANT_SLUG,
                    "OMR callback references unknown tenant " + request.tenantSlug()
                            + " for job " + request.jobId()
            );
        }
        TenantContextHolder.withinContext(request.tenantSlug(), tenantId, (Runnable) () -> {
            try {
                processor.processGradingCallback(request);
                log.info("Background job callback processed omr_grading jobId={}",
                        request.jobId(),
                        field("eventType", "background_job"),
                        field("jobName", "omr_grading"),
                        field("jobPhase", "callback_received"),
                        field("jobId", request.jobId()),
                        field("tenantSlug", request.tenantSlug()),
                        field("entityType", "exam"),
                        field("entityId", request.examId()),
                        field("queue", RabbitMqConstants.GRADING_OMR_CALLBACK_QUEUE),
                        field("latencyMs", elapsedMillis(startNanos)),
                        field("outcome", "success"));
            } catch (RuntimeException e) {
                log.error(
                        "OMR callback processing failed for job {} (tenant {}): {}",
                        request.jobId(), request.tenantSlug(), e.getMessage(),
                        field("eventType", "background_job"),
                        field("jobName", "omr_grading"),
                        field("jobPhase", "callback_failed"),
                        field("jobId", request.jobId()),
                        field("tenantSlug", request.tenantSlug()),
                        field("entityType", "exam"),
                        field("entityId", request.examId()),
                        field("queue", RabbitMqConstants.GRADING_OMR_CALLBACK_QUEUE),
                        field("latencyMs", elapsedMillis(startNanos)),
                        field("outcome", "failed"),
                        field("exceptionClass", e.getClass().getSimpleName()),
                        e
                );
                throw e;
            }
        });
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
