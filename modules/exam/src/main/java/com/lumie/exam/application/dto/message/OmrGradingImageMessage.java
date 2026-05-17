package com.lumie.exam.application.dto.message;

import java.io.Serializable;

public record OmrGradingImageMessage(
        Long jobId,
        Long examId,
        String tenantSlug,
        String imageKey,
        int imageIndex,
        int totalImages,
        int schemaVersion
) implements Serializable {
}
