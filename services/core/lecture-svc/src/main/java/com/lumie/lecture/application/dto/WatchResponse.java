package com.lumie.lecture.application.dto;

public record WatchResponse(
        Long lectureId,
        String title,
        String description,
        String youtubeUrl,
        String visibility
) {}
