package com.lumie.notification.application.dto;

import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.vo.Recipient;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsStatus;

import java.time.Instant;
import com.lumie.common.util.Timestamps;
import java.util.List;

public record SmsMessageResponse(
    Long id,
    Long senderId,
    RecipientType recipientType,
    List<Recipient> recipients,
    String title,
    String content,
    Long templateId,
    Instant sentAt,
    SmsStatus status,
    int successCount,
    int failCount,
    Instant createdAt,
    Instant updatedAt
) {
    public static SmsMessageResponse from(SmsMessage entity) {
        return new SmsMessageResponse(
            entity.getId(),
            entity.getSenderId(),
            entity.getRecipientType(),
            entity.getRecipients(),
            entity.getTitle(),
            entity.getContent(),
            entity.getTemplateId(),
            Timestamps.utcInstant(entity.getSentAt()),
            entity.getStatus(),
            entity.getSuccessCount(),
            entity.getFailCount(),
            Timestamps.utcInstant(entity.getCreatedAt()),
            Timestamps.utcInstant(entity.getUpdatedAt())
        );
    }
}
