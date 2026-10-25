package com.lumie.content.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import com.lumie.file.api.TemporaryAttachment;

public record CreateQnaRequest(
        // Accepted temporarily for old clients, but never used as the author.
        @Schema(hidden = true)
        Long qnaUserId,

        @NotBlank(message = "Title is required")
        String qnaTitle,

        @NotBlank(message = "Content is required")
        String qnaContent,

        List<TemporaryAttachment> temporaryAttachments,
        List<java.util.UUID> uploadSessionIds
) {
    public CreateQnaRequest(Long qnaUserId, String qnaTitle, String qnaContent,
                            List<TemporaryAttachment> temporaryAttachments) {
        this(qnaUserId, qnaTitle, qnaContent, temporaryAttachments, null);
    }
}
