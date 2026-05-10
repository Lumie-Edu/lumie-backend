package com.lumie.notification.application.dto;

import com.lumie.notification.domain.entity.SmsTemplate;
import com.lumie.notification.domain.vo.TemplateCategory;

import java.time.Instant;
import com.lumie.common.util.Timestamps;

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
            Timestamps.utcInstant(entity.getCreatedAt()),
            Timestamps.utcInstant(entity.getUpdatedAt())
        );
    }
}
