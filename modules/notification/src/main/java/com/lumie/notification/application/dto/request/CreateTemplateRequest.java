package com.lumie.notification.application.dto.request;

import com.lumie.notification.domain.vo.TemplateCategory;
import jakarta.validation.constraints.NotBlank;

public record CreateTemplateRequest(
    @NotBlank(message = "Template name is required")
    String name,

    @NotBlank(message = "Template content is required")
    String content,

    TemplateCategory category
) {
}
