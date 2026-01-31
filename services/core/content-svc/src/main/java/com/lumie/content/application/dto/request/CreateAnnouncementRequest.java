package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateAnnouncementRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Content is required")
        String content,

        Long authorId,

        String authorName,

        Boolean isImportant
) {
}
