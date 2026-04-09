package com.lumie.lecture.application.dto;

import java.util.List;

public record WatchResponse(
        Long lectureId,
        String title,
        String description,
        String youtubeUrl,
        String transcript,
        String aiSummary,
        List<String> topicsExtracted,
        Integer videoDuration,
        String visibility
) {}
