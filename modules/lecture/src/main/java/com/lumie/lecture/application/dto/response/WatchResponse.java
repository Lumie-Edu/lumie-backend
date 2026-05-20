package com.lumie.lecture.application.dto.response;

public record WatchResponse(
        Long lectureId,
        String title,
        String description,
        String youtubeUrl,
        String visibility
) {}
