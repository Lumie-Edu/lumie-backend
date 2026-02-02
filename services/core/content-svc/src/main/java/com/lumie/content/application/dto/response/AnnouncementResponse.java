package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Announcement;

import java.time.LocalDateTime;
import java.util.Set;

public record AnnouncementResponse(
        Long id,
        Long authorId,
        String announcementTitle,
        String announcementContent,
        Boolean isItAssetAnnouncement,
        Boolean isItImportantAnnouncement,
        Set<Long> academyIds,
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
                announcement.getAcademyIds(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}
