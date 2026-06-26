package com.lumie.notification.application.dto.request;

import com.lumie.notification.domain.vo.RecipientRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record SendToClassRequest(
    @NotNull(message = "Recipient role is required")
    RecipientRole recipientRole,

    @Size(max = 200, message = "Title must be at most 200 characters")
    String title,

    @NotBlank(message = "Content is required")
    @Size(max = 2000, message = "Content must be at most 2000 characters")
    String content,

    Long templateId,

    Instant scheduledAt
) {
    public SendToClassRequest(RecipientRole recipientRole, String title, String content, Long templateId) {
        this(recipientRole, title, content, templateId, null);
    }
}
