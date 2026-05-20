package com.lumie.notification.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendToClassRequest(
    @NotNull(message = "Sender ID is required")
    Long senderId,

    String title,

    @NotBlank(message = "Content is required")
    String content,

    Long templateId
) {
}
