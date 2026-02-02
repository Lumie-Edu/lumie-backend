package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
        Long studentId,

        Long adminId,

        @NotBlank(message = "Content is required")
        String commentContent
) {
}
