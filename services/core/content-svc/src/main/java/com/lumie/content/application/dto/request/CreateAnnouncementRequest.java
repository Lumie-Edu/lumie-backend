package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record CreateAnnouncementRequest(
        @NotNull(message = "Author ID is required")
        Long authorId,

        @NotBlank(message = "Title is required")
        String announcementTitle,

        @NotBlank(message = "Content is required")
        String announcementContent,

        Boolean isItAssetAnnouncement,

        Boolean isItImportantAnnouncement,

        Set<Long> academyIds
) {
}
