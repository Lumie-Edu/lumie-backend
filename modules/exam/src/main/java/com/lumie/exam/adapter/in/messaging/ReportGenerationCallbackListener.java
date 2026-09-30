package com.lumie.exam.adapter.in.messaging;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.request.ReportCallbackRequest;
import com.lumie.exam.application.service.ReportJobService;
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
 * Consumes report-svc generation callbacks from RabbitMQ.
 *
 * <p>Replaces the previous {@code @PostMapping("/internal/reports/callback")}
 * HTTP handler. See {@link OmrGradingCallbackListener} for the
 * architectural rationale; this is the report-svc equivalent and
 * shares the same {@code report.#} DLQ binding semantics.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportGenerationCallbackListener {

    private final ReportJobService reportJobService;
    private final TenantService tenantService;

    @RabbitListener(queues = RabbitMqConstants.REPORT_CALLBACK_QUEUE)
    public void handle(ReportCallbackRequest request) {
        long startNanos = System.nanoTime();
        if (request.tenantSlug() == null || request.tenantSlug().isBlank()) {
            log.error("Background job callback rejected report_generation jobId={} missing tenantSlug",
                    request.jobId(),
                    field("eventType", "background_job"),
                    field("jobName", "report_generation"),
                    field("jobPhase", "callback_rejected"),
                    field("jobId", request.jobId()),
                    field("entityType", "exam"),
                    field("entityId", request.examId()),
                    field("queue", RabbitMqConstants.REPORT_CALLBACK_QUEUE),
                    field("latencyMs", elapsedMillis(startNanos)),
                    field("outcome", "failed"),
                    field("errorCode", ExamErrorCode.MISSING_TENANT_SLUG.name()));
            throw new BusinessException(
                    ExamErrorCode.MISSING_TENANT_SLUG,
                    "Report callback missing tenantSlug for job " + request.jobId()
            );
        }
        Long tenantId = tenantService.getTenantBySlug(request.tenantSlug())
                .map(TenantService.TenantData::id)
                .orElse(null);
        if (tenantId == null) {
            log.error("Background job callback rejected report_generation jobId={} unknown tenant",
                    request.jobId(),
                    field("eventType", "background_job"),
                    field("jobName", "report_generation"),
                    field("jobPhase", "callback_rejected"),
                    field("jobId", request.jobId()),
                    field("tenantSlug", request.tenantSlug()),
                    field("entityType", "exam"),
                    field("entityId", request.examId()),
                    field("queue", RabbitMqConstants.REPORT_CALLBACK_QUEUE),
                    field("latencyMs", elapsedMillis(startNanos)),
                    field("outcome", "failed"),
                    field("errorCode", ExamErrorCode.MISSING_TENANT_SLUG.name()));
            throw new BusinessException(
                    ExamErrorCode.MISSING_TENANT_SLUG,
                    "Report callback references unknown tenant " + request.tenantSlug()
                            + " for job " + request.jobId()
            );
        }
        TenantContextHolder.withinContext(request.tenantSlug(), tenantId, (Runnable) () -> {
            try {
                reportJobService.processCallback(request);
                log.info("Background job callback processed report_generation jobId={}",
                        request.jobId(),
                        field("eventType", "background_job"),
                        field("jobName", "report_generation"),
                        field("jobPhase", "callback_received"),
                        field("jobId", request.jobId()),
                        field("tenantSlug", request.tenantSlug()),
                        field("entityType", "exam"),
                        field("entityId", request.examId()),
                        field("queue", RabbitMqConstants.REPORT_CALLBACK_QUEUE),
                        field("latencyMs", elapsedMillis(startNanos)),
                        field("outcome", "success"));
            } catch (RuntimeException e) {
                log.error(
                        "Report callback processing failed for job {} (tenant {}): {}",
                        request.jobId(), request.tenantSlug(), e.getMessage(),
                        field("eventType", "background_job"),
                        field("jobName", "report_generation"),
                        field("jobPhase", "callback_failed"),
                        field("jobId", request.jobId()),
                        field("tenantSlug", request.tenantSlug()),
                        field("entityType", "exam"),
                        field("entityId", request.examId()),
                        field("queue", RabbitMqConstants.REPORT_CALLBACK_QUEUE),
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
