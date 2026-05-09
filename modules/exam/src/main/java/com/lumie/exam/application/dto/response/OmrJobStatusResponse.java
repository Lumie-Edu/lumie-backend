package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.OmrGradingJob;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record OmrJobStatusResponse(
        Long jobId,
        Long examId,
        String status,
        int totalImages,
        int processedImages,
        int successCount,
        int failCount,
        int savedCount,
        List<Map<String, Object>> results,
        LocalDateTime createdAt
) {
    public static OmrJobStatusResponse from(OmrGradingJob job) {
        return new OmrJobStatusResponse(
                job.getId(),
                job.getExam().getId(),
                job.getStatus().name(),
                job.getTotalImages(),
                job.getProcessedImages(),
                job.getSuccessCount(),
                job.getFailCount(),
                job.getSavedCount(),
                job.getResults(),
                job.getCreatedAt()
        );
    }
}
