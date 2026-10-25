package com.lumie.content.application.dto.request;

import java.util.List;
import java.util.UUID;
import com.lumie.file.api.TemporaryAttachment;
import com.lumie.content.domain.vo.AnnouncementAudienceScope;

public record UpdateAnnouncementRequest(
        String announcementTitle,
        String announcementContent,
        Boolean isItAssetAnnouncement,
        Boolean isItImportantAnnouncement,
        List<UUID> attachmentFileIds,
        List<TemporaryAttachment> temporaryAttachments,
        List<UUID> uploadSessionIds,
        List<Long> classIds,
        AnnouncementAudienceScope audienceScope
) {
    public UpdateAnnouncementRequest(String announcementTitle, String announcementContent,
                                     Boolean isItAssetAnnouncement, Boolean isItImportantAnnouncement,
                                     List<UUID> attachmentFileIds, List<TemporaryAttachment> temporaryAttachments,
                                     List<Long> classIds) {
        this(announcementTitle, announcementContent, isItAssetAnnouncement,
                isItImportantAnnouncement, attachmentFileIds, temporaryAttachments, null, classIds, null);
    }

    public UpdateAnnouncementRequest(String announcementTitle, String announcementContent,
                                     Boolean isItAssetAnnouncement, Boolean isItImportantAnnouncement,
                                     List<UUID> attachmentFileIds, List<TemporaryAttachment> temporaryAttachments,
                                     List<Long> classIds, AnnouncementAudienceScope audienceScope) {
        this(announcementTitle, announcementContent, isItAssetAnnouncement, isItImportantAnnouncement,
                attachmentFileIds, temporaryAttachments, null, classIds, audienceScope);
    }
}
