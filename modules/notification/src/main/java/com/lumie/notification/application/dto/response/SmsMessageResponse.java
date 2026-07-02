package com.lumie.notification.application.dto.response;

import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsMessageType;
import com.lumie.notification.domain.vo.SmsStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record SmsMessageResponse(
    Long id,
    @Schema(nullable = true)
    Long senderId,
    @Schema(nullable = true)
    String senderNumber,
    RecipientType recipientType,
    List<RecipientResponse> recipients,
    int recipientCount,
    @Schema(nullable = true) String title,
    @Schema(nullable = true) Long recipientGroupId,
    @Schema(nullable = true) String content,
    @Schema(nullable = true) Long templateId,
    @Schema(nullable = true) Instant scheduledAt,
    @Schema(nullable = true) Instant cancelledAt,
    @Schema(nullable = true) Instant sentAt,
    SmsMessageType messageType,
    int estimatedSegments,
    int unitPrice,
    int totalPrice,
    SmsStatus status,
    int successCount,
    int failCount,
    Instant createdAt,
    Instant updatedAt
) {
    private static final String REDACTED_CONTENT_PREVIEW = "[redacted]";

    public SmsMessageResponse(Long id, Long senderId, RecipientType recipientType,
                              List<RecipientResponse> recipients, String title, String content,
                              Long templateId, Instant sentAt, SmsStatus status, int successCount,
                              int failCount, Instant createdAt, Instant updatedAt) {
        this(id, senderId, null, recipientType, recipients, recipients.size(), title, null, content, templateId, null, null, sentAt,
                SmsMessageType.SMS, 1, 0, 0, status, successCount, failCount, createdAt, updatedAt);
    }

    public static SmsMessageResponse from(SmsMessage entity) {
        return new SmsMessageResponse(
            entity.getId(),
            entity.getSenderId(),
            entity.getSenderNumber(),
            entity.getRecipientType(),
            entity.getRecipients().stream().map(RecipientResponse::from).toList(),
            entity.getRecipients().size(),
            entity.getTitle(),
            entity.getRecipientGroupId(),
            entity.getContent(),
            entity.getTemplateId(),
            entity.getScheduledAt(),
            entity.getCancelledAt(),
            entity.getSentAt(),
            entity.getMessageType(),
            entity.getEstimatedSegments(),
            entity.getUnitPrice(),
            entity.getTotalPrice(),
            entity.getStatus(),
            entity.getSuccessCount(),
            entity.getFailCount(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public static SmsMessageResponse summary(SmsMessage entity) {
        return new SmsMessageResponse(
            entity.getId(),
            entity.getSenderId(),
            entity.getSenderNumber(),
            entity.getRecipientType(),
            List.of(),
            entity.getRecipients().size(),
            entity.getTitle(),
            entity.getRecipientGroupId(),
            previewContent(entity.getContent()),
            entity.getTemplateId(),
            entity.getScheduledAt(),
            entity.getCancelledAt(),
            entity.getSentAt(),
            entity.getMessageType(),
            entity.getEstimatedSegments(),
            entity.getUnitPrice(),
            entity.getTotalPrice(),
            entity.getStatus(),
            entity.getSuccessCount(),
            entity.getFailCount(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private static String previewContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        return REDACTED_CONTENT_PREVIEW;
    }
}
