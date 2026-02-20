package com.lumie.exam.application.dto.message;

import java.io.Serializable;
import java.util.List;

public record OmrGradingJobMessage(
        Long jobId,
        Long examId,
        String tenantSlug,
        List<String> imageKeys
) implements Serializable {
}
