package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
        @NotBlank(message = "Content is required")
        String commentContent
) {
}
