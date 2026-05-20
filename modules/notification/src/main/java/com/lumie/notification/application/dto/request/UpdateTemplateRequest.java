package com.lumie.notification.application.dto.request;

import com.lumie.notification.domain.vo.TemplateCategory;

public record UpdateTemplateRequest(
    String name,
    String content,
    TemplateCategory category
) {
}
