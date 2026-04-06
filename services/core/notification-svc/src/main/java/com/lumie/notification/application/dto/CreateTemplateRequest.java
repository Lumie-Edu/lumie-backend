package com.lumie.notification.application.dto;

import com.lumie.notification.domain.vo.TemplateCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTemplateRequest(
    @NotNull(message = "Academy ID is required")
    Long academyId,

    @NotBlank(message = "Template name is required")
    String name,

    @NotBlank(message = "Template content is required")
    String content,

    TemplateCategory category
) {
}
