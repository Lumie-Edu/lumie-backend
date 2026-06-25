package com.lumie.notification.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record SendToClassRequest(
    @Size(max = 200, message = "Title must be at most 200 characters")
    String title,

    @NotBlank(message = "Content is required")
    @Size(max = 2000, message = "Content must be at most 2000 characters")
    String content,

    Long templateId,

    Instant scheduledAt
) {
    public SendToClassRequest(String title, String content, Long templateId) {
        this(title, content, templateId, null);
    }
}
