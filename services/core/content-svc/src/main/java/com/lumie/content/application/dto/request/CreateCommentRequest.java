package com.lumie.content.application.dto.request;

import com.lumie.content.domain.vo.AuthorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCommentRequest(
        @NotBlank(message = "Content is required")
        String content,

        @NotNull(message = "Author ID is required")
        Long authorId,

        String authorName,

        @NotNull(message = "Author type is required")
        AuthorType authorType
) {
}
