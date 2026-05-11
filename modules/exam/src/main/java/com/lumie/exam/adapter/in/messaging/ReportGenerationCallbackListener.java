package com.lumie.exam.adapter.in.messaging;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.request.ReportCallbackRequest;
import com.lumie.exam.application.service.ReportJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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

    @RabbitListener(queues = "report.generation-callback")
    public void handle(ReportCallbackRequest request) {
        if (request.tenantSlug() == null || request.tenantSlug().isBlank()) {
            throw new IllegalArgumentException(
                    "Report callback missing tenantSlug for job " + request.jobId()
                            + " — rejecting to DLQ"
            );
        }
        TenantContextHolder.withinContext(request.tenantSlug(), null, (Runnable) () -> {
            try {
                reportJobService.processCallback(request);
            } catch (RuntimeException e) {
                log.error(
                        "Report callback processing failed for job {} (tenant {}): {}",
                        request.jobId(), request.tenantSlug(), e.getMessage(), e
                );
                throw e;
            }
        });
    }
}
