package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQnaRequest(
        Long academyId,

        @NotNull(message = "Author ID is required")
        Long authorId,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Content is required")
        String content,

        String category,

        Boolean isPrivate
) {
}
