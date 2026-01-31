package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Announcement;

import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id,
        Long academyId,
        Long authorId,
        String title,
        String content,
        Boolean isImportant,
        Boolean isPublic,
        Integer viewCount,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AnnouncementResponse from(Announcement announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getAcademyId(),
                announcement.getAuthorId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getIsImportant(),
                announcement.getIsPublic(),
                announcement.getViewCount(),
                announcement.getPublishedAt(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}
