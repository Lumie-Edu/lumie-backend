package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Announcement;

import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id,
        String title,
        String content,
        Long authorId,
        String authorName,
        Boolean isImportant,
        Integer viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AnnouncementResponse from(Announcement announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getAuthorId(),
                announcement.getAuthorName(),
                announcement.getIsImportant(),
                announcement.getViewCount(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}
