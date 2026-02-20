package com.lumie.exam.adapter.in.messaging;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.message.OmrGradingJobMessage;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.application.service.OmrGradingJobProcessor;
import com.lumie.messaging.config.RabbitMqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OmrGradingJobListener {

    private final OmrGradingJobProcessor processor;
    private final OmrStoragePort storagePort;

    @RabbitListener(queues = RabbitMqConstants.GRADING_OMR_REQUEST_QUEUE)
    public void handleOmrGradingJob(OmrGradingJobMessage message) {
        log.info("Received OMR grading job: jobId={}, examId={}, images={}",
                message.jobId(), message.examId(), message.imageKeys().size());

        try {
            TenantContextHolder.setTenant(message.tenantSlug());

            processor.startProcessing(message.jobId());

            List<Map<String, Object>> results = new ArrayList<>();
            for (String imageKey : message.imageKeys()) {
                Map<String, Object> result = processor.processImageAndUpdateProgress(
                        message.jobId(), message.examId(), imageKey, message.tenantSlug());
                results.add(result);
            }

            processor.completeJob(message.jobId(), results);

            // Cleanup MinIO images
            try {
                storagePort.deleteObjects(message.imageKeys());
            } catch (Exception e) {
                log.warn("Failed to cleanup MinIO images for job: {}", message.jobId(), e);
            }

            log.info("OMR grading job completed: jobId={}", message.jobId());

        } catch (Exception e) {
            log.error("OMR grading job failed: jobId={}", message.jobId(), e);
            try {
                processor.failJob(message.jobId());
            } catch (Exception ex) {
                log.error("Failed to mark job as failed: {}", message.jobId(), ex);
            }
        } finally {
            TenantContextHolder.clear();
        }
    }
}
