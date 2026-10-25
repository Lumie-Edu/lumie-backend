package com.lumie.content.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import com.lumie.file.api.TemporaryAttachment;
import com.lumie.content.domain.vo.AnnouncementAudienceScope;

public record CreateAnnouncementRequest(
        // Accepted temporarily for old clients, but never used as the author.
        @Schema(hidden = true)
        Long authorId,

        @NotBlank(message = "Title is required")
        String announcementTitle,

        @NotBlank(message = "Content is required")
        String announcementContent,

        Boolean isItAssetAnnouncement,

        Boolean isItImportantAnnouncement,

        List<TemporaryAttachment> temporaryAttachments,

        List<java.util.UUID> uploadSessionIds,

        List<Long> classIds,

        AnnouncementAudienceScope audienceScope
) {
    public CreateAnnouncementRequest(Long authorId, String announcementTitle, String announcementContent,
                                     Boolean isItAssetAnnouncement, Boolean isItImportantAnnouncement,
                                     List<TemporaryAttachment> temporaryAttachments, List<Long> classIds) {
        this(authorId, announcementTitle, announcementContent, isItAssetAnnouncement,
                isItImportantAnnouncement, temporaryAttachments, null, classIds, null);
    }

    public CreateAnnouncementRequest(Long authorId, String announcementTitle, String announcementContent,
                                     Boolean isItAssetAnnouncement, Boolean isItImportantAnnouncement,
                                     List<TemporaryAttachment> temporaryAttachments, List<Long> classIds,
                                     AnnouncementAudienceScope audienceScope) {
        this(authorId, announcementTitle, announcementContent, isItAssetAnnouncement,
                isItImportantAnnouncement, temporaryAttachments, null, classIds, audienceScope);
    }
}
