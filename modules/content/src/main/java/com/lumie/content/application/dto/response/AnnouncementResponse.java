package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Announcement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record AnnouncementResponse(
        Long id,
        @Schema(nullable = true) Long authorId,
        String authorName,
        String announcementTitle,
        String announcementContent,
        @Schema(nullable = true) Boolean isItAssetAnnouncement,
        @Schema(nullable = true) Boolean isItImportantAnnouncement,
        Instant createdAt,
        Instant updatedAt,
        List<Long> classIds
) {
    public static AnnouncementResponse from(Announcement announcement) {
        return from(announcement, null, List.of());
    }

    public static AnnouncementResponse from(Announcement announcement, String authorName) {
        return from(announcement, authorName, List.of());
    }

    public static AnnouncementResponse from(Announcement announcement, String authorName, List<Long> classIds) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getAuthorId(),
                authorName,
                announcement.getAnnouncementTitle(),
                announcement.getAnnouncementContent(),
                announcement.getIsItAssetAnnouncement(),
                announcement.getIsItImportantAnnouncement(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt(),
                classIds == null ? List.of() : classIds
        );
    }
}
