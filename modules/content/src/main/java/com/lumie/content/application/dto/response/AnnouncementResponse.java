package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Announcement;

import java.time.Instant;
import com.lumie.common.util.Timestamps;

public record AnnouncementResponse(
        Long id,
        Long authorId,
        String authorName,
        String announcementTitle,
        String announcementContent,
        Boolean isItAssetAnnouncement,
        Boolean isItImportantAnnouncement,
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
                Timestamps.utcInstant(announcement.getCreatedAt()),
                Timestamps.utcInstant(announcement.getUpdatedAt())
        );
    }
}
