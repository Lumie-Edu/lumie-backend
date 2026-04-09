package com.lumie.lecture.application.dto;

public record WatchResponse(
        Long lectureId,
        String title,
        String description,
        String videoUrl,
        String subtitleUrl,
        String transcript,
        String aiSummary,
        java.util.List<String> topicsExtracted,
        Integer videoDuration,
        String visibility
) {}
