package com.lumie.notification.application.dto.response;

import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record SmsMessageResponse(
    Long id,
    @Schema(nullable = true)
    Long senderId,
    RecipientType recipientType,
    List<RecipientResponse> recipients,
    @Schema(nullable = true) String title,
    String content,
    @Schema(nullable = true) Long templateId,
    @Schema(nullable = true) Instant sentAt,
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
            entity.getRecipients().stream().map(RecipientResponse::from).toList(),
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
