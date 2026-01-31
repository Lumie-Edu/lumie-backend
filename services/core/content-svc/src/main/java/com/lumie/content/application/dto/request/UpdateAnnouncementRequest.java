package com.lumie.content.application.dto.request;

public record UpdateAnnouncementRequest(
        String title,
        String content,
        Boolean isPinned,
        Boolean isPublic
) {
}
