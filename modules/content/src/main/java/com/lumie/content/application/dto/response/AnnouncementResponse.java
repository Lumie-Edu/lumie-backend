package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Announcement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record AnnouncementResponse(
        Long id,
        Long authorId,
        String authorName,
        String announcementTitle,
        String announcementContent,
        @Schema(nullable = true) Boolean isItAssetAnnouncement,
        @Schema(nullable = true) Boolean isItImportantAnnouncement,
        Instant createdAt,
        Instant updatedAt
) {
    public static AnnouncementResponse from(Announcement announcement) {
        return from(announcement, null);
    }

    public static AnnouncementResponse from(Announcement announcement, String authorName) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getAuthorId(),
                authorName,
                announcement.getAnnouncementTitle(),
                announcement.getAnnouncementContent(),
                announcement.getIsItAssetAnnouncement(),
                announcement.getIsItImportantAnnouncement(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}
