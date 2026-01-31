package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateAnnouncementRequest(
        Long academyId,

        Long authorId,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Content is required")
        String content,

        Boolean isImportant,

        Boolean isPublic
) {
}
