package com.lumie.exam.adapter.in.messaging;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.message.OmrGradingImageMessage;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.application.service.OmrGradingJobProcessor;
import com.lumie.exam.domain.entity.OmrGradingJob;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.messaging.config.RabbitMqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OmrGradingJobListener {

    private final OmrGradingJobProcessor processor;
    private final OmrGradingJobRepository jobRepository;
    private final OmrStoragePort storagePort;

    @RabbitListener(
            queues = RabbitMqConstants.GRADING_OMR_REQUEST_QUEUE,
            concurrency = "5-10"
    )
    public void handleOmrGradingImage(OmrGradingImageMessage message) {
        log.info("Processing OMR image: jobId={}, imageIndex={}/{}",
                message.jobId(), message.imageIndex() + 1, message.totalImages());

        try {
            TenantContextHolder.setTenant(message.tenantSlug());

            jobRepository.startProcessingIfPending(message.jobId());

            Map<String, Object> result = processor.processSingleImage(
                    message.jobId(), message.examId(), message.imageKey(), message.tenantSlug());

            boolean success = Boolean.TRUE.equals(result.get("success"));
            if (success) {
                jobRepository.incrementSuccess(message.jobId());
            } else {
                jobRepository.incrementFail(message.jobId());
            }
            jobRepository.appendResult(message.jobId(), result);

            if (jobRepository.tryComplete(message.jobId(), message.totalImages())) {
                log.info("OMR grading job completed: jobId={}", message.jobId());
                cleanupImages(message.jobId());
            }

        } catch (Exception e) {
            log.error("Failed to process OMR image: jobId={}, imageKey={}",
                    message.jobId(), message.imageKey(), e);
            try {
                jobRepository.incrementFail(message.jobId());
                if (jobRepository.tryComplete(message.jobId(), message.totalImages())) {
                    cleanupImages(message.jobId());
                }
            } catch (Exception ex) {
                log.error("Failed to update job status: jobId={}", message.jobId(), ex);
            }
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void cleanupImages(Long jobId) {
        try {
            OmrGradingJob job = jobRepository.findById(jobId).orElse(null);
            if (job != null && job.getImageKeys() != null) {
                storagePort.deleteObjects(job.getImageKeys());
                log.info("Cleaned up {} MinIO images for job: {}", job.getImageKeys().size(), jobId);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup MinIO images for job: {}", jobId, e);
        }
    }
}
