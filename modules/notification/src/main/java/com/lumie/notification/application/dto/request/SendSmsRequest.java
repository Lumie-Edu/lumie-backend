package com.lumie.notification.application.dto.request;

import com.lumie.notification.domain.vo.Recipient;
import com.lumie.notification.domain.vo.RecipientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SendSmsRequest(
    @NotNull(message = "Sender ID is required")
    Long senderId,

    @NotNull(message = "Recipient type is required")
    RecipientType recipientType,

    @NotNull(message = "Recipients are required")
    List<Recipient> recipients,

    String title,

    @NotBlank(message = "Content is required")
    String content,

    Long templateId
) {
}
