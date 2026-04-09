package com.lumie.lecture.application.dto;

import java.util.List;

public record ProcessingCallbackRequest(
        Long lectureId,
        String tenantSlug,
        boolean success,
        String transcript,
        String subtitleFileKey,
        String aiSummary,
        List<String> topicsExtracted,
        Integer videoDuration,
        String errorMessage
) {}
