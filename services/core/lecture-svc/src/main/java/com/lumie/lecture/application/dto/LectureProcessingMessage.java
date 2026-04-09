package com.lumie.lecture.application.dto;

public record LectureProcessingMessage(
        Long lectureId,
        String tenantSlug,
        String youtubeUrl
) {}
