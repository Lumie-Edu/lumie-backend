package com.lumie.content.application.dto.request;

import java.util.List;
import java.util.UUID;
import com.lumie.file.api.TemporaryAttachment;

public record UpdateAnnouncementRequest(
        String announcementTitle,
        String announcementContent,
        Boolean isItAssetAnnouncement,
        Boolean isItImportantAnnouncement,
        List<UUID> attachmentFileIds,
        List<TemporaryAttachment> temporaryAttachments,
        List<Long> classIds
) {
}
