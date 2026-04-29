package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Announcement;

import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id,
        Long authorId,
        String announcementTitle,
        String announcementContent,
        Boolean isItAssetAnnouncement,
        Boolean isItImportantAnnouncement,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AnnouncementResponse from(Announcement announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getAuthorId(),
                announcement.getAnnouncementTitle(),
                announcement.getAnnouncementContent(),
                announcement.getIsItAssetAnnouncement(),
                announcement.getIsItImportantAnnouncement(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}
