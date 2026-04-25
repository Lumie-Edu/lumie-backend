package com.lumie.content.application.dto.request;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record UpdateAnnouncementRequest(
        String announcementTitle,
        String announcementContent,
        Boolean isItAssetAnnouncement,
        Boolean isItImportantAnnouncement,
        Set<Long> academyIds,
        List<UUID> fileIdsToDelete
) {
}
