package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import com.lumie.file.api.TemporaryAttachment;

public record CreateQnaRequest(
        @NotNull(message = "User ID is required")
        Long qnaUserId,

        @NotBlank(message = "Title is required")
        String qnaTitle,

        @NotBlank(message = "Content is required")
        String qnaContent,

        List<TemporaryAttachment> temporaryAttachments
) {
}
