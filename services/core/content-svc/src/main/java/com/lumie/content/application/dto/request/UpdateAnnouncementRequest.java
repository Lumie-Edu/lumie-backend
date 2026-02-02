package com.lumie.content.application.dto.request;

import java.util.Set;

public record UpdateAnnouncementRequest(
        String announcementTitle,
        String announcementContent,
        Boolean isItAssetAnnouncement,
        Boolean isItImportantAnnouncement,
        Set<Long> academyIds
) {
}
