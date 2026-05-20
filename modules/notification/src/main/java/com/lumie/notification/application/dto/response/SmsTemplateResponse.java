package com.lumie.notification.application.dto.response;

import com.lumie.notification.domain.entity.SmsTemplate;
import com.lumie.notification.domain.vo.TemplateCategory;

import java.time.Instant;

public record SmsTemplateResponse(
    Long id,
    String name,
    String content,
    TemplateCategory category,
    Instant createdAt,
    Instant updatedAt
) {
    public static SmsTemplateResponse from(SmsTemplate entity) {
        return new SmsTemplateResponse(
            entity.getId(),
            entity.getName(),
            entity.getContent(),
            entity.getCategory(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
