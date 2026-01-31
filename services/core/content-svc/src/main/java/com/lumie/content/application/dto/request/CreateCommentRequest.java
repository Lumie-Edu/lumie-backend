package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCommentRequest(
        @NotNull(message = "Author ID is required")
        Long authorId,

        @NotBlank(message = "Content is required")
        String content,

        Boolean isAnswer
) {
}
