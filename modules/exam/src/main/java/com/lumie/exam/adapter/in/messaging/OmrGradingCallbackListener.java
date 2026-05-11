package com.lumie.exam.adapter.in.messaging;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.request.OmrGradingCallbackRequest;
import com.lumie.exam.application.service.OmrGradingJobProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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

    @RabbitListener(queues = "grading.omr-callback")
    public void handle(OmrGradingCallbackRequest request) {
        TenantContextHolder.withinContext(request.tenantSlug(), null, (Runnable) () -> {
            try {
                processor.processGradingCallback(request);
            } catch (RuntimeException e) {
                log.error(
                        "OMR callback processing failed for job {} (tenant {}): {}",
                        request.jobId(), request.tenantSlug(), e.getMessage(), e
                );
                throw e;
            }
        });
    }
}
