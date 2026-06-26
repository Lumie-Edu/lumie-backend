package com.lumie.notification.application.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record SendSmsRequest(
    @Valid
    @Size(max = 500, message = "Recipients list must be at most 500 entries")
    List<@Valid RecipientRequest> recipients,

    @Size(max = 200, message = "Title must be at most 200 characters")
    String title,

    Long recipientGroupId,

    @NotBlank(message = "Content is required")
    @Size(max = 2000, message = "Content must be at most 2000 characters")
    String content,

    Long templateId,

    Instant scheduledAt
) {
    public SendSmsRequest(List<RecipientRequest> recipients,
                          String title, String content, Long templateId) {
        this(recipients, title, null, content, templateId, null);
    }

    public SendSmsRequest(List<RecipientRequest> recipients,
                          String title, String content, Long templateId, Instant scheduledAt) {
        this(recipients, title, null, content, templateId, scheduledAt);
    }
}
