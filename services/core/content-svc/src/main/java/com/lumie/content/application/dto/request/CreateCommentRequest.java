package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
        Long studentId,

        Long userId,

        @NotBlank(message = "Content is required")
        String commentContent
) {
}
