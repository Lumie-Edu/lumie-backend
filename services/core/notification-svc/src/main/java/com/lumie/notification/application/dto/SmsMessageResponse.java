package com.lumie.notification.application.dto;

import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.vo.Recipient;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SmsMessageResponse(
    Long id,
    Long academyId,
    Long senderId,
    RecipientType recipientType,
    List<Recipient> recipients,
    String title,
    String content,
    Long templateId,
    LocalDateTime sentAt,
    SmsStatus status,
    int successCount,
    int failCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static SmsMessageResponse from(SmsMessage entity) {
        return new SmsMessageResponse(
            entity.getId(),
            entity.getAcademyId(),
            entity.getSenderId(),
            entity.getRecipientType(),
            entity.getRecipients(),
            entity.getTitle(),
            entity.getContent(),
            entity.getTemplateId(),
            entity.getSentAt(),
            entity.getStatus(),
            entity.getSuccessCount(),
            entity.getFailCount(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
