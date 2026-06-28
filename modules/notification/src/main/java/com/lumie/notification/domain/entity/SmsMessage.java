package com.lumie.notification.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.common.exception.BusinessException;
import com.lumie.notification.domain.exception.SmsErrorCode;
import com.lumie.notification.domain.vo.Recipient;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsMessageType;
import com.lumie.notification.domain.vo.SmsStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "sms_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SmsMessage extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "sender_id")
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 20)
    private RecipientType recipientType;

    @Convert(converter = RecipientsConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recipients", nullable = false, columnDefinition = "jsonb")
    private List<Recipient> recipients;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "recipient_group_id")
    private Long recipientGroupId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "provider", length = 30)
    private String provider;

    @Column(name = "provider_group_id", length = 80)
    private String providerGroupId;

    @Column(name = "dispatch_attempt_count", nullable = false)
    private int dispatchAttemptCount;

    @Column(name = "last_provider_status", length = 50)
    private String lastProviderStatus;

    @Column(name = "last_provider_error", columnDefinition = "TEXT")
    private String lastProviderError;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 10)
    private SmsMessageType messageType;

    @Column(name = "estimated_segments", nullable = false)
    private int estimatedSegments;

    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    @Column(name = "total_price", nullable = false)
    private int totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SmsStatus status;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Builder
    private SmsMessage(Long senderId, RecipientType recipientType,
                       List<Recipient> recipients, String title, Long recipientGroupId, String content,
                       Long templateId, Instant scheduledAt, SmsMessageType messageType,
                       int estimatedSegments, int unitPrice, int totalPrice, SmsStatus status) {
        this.senderId = senderId;
        this.recipientType = recipientType;
        this.recipients = recipients;
        this.title = title;
        this.recipientGroupId = recipientGroupId;
        this.content = content;
        this.templateId = templateId;
        this.scheduledAt = scheduledAt;
        this.messageType = messageType != null ? messageType : SmsMessageType.SMS;
        this.estimatedSegments = Math.max(1, estimatedSegments);
        this.unitPrice = Math.max(0, unitPrice);
        this.totalPrice = Math.max(0, totalPrice);
        this.status = status != null ? status : SmsStatus.PENDING;
        this.successCount = 0;
        this.failCount = 0;
    }

    public static SmsMessage create(Long senderId, RecipientType recipientType,
                                    List<Recipient> recipients, String title, String content,
                                    Long templateId, Instant scheduledAt, SmsMessageType messageType,
                                    int estimatedSegments, int unitPrice, int totalPrice) {
        return create(senderId, recipientType, recipients, title, null, content, templateId,
                scheduledAt, messageType, estimatedSegments, unitPrice, totalPrice);
    }

    public static SmsMessage create(Long senderId, RecipientType recipientType,
                                    List<Recipient> recipients, String title, Long recipientGroupId, String content,
                                    Long templateId, Instant scheduledAt, SmsMessageType messageType,
                                    int estimatedSegments, int unitPrice, int totalPrice) {
        return SmsMessage.builder()
                .senderId(senderId)
                .recipientType(recipientType)
                .recipients(recipients)
                .title(title)
                .recipientGroupId(recipientGroupId)
                .content(content)
                .templateId(templateId)
                .scheduledAt(scheduledAt)
                .messageType(messageType)
                .estimatedSegments(estimatedSegments)
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .status(SmsStatus.PENDING)
                .build();
    }

    public static SmsMessage create(Long senderId, RecipientType recipientType,
                                    List<Recipient> recipients, String title, String content,
                                    Long templateId) {
        return create(senderId, recipientType, recipients, title, content, templateId,
                null, SmsMessageType.SMS, 1, 0, 0);
    }

    public void markAsSent() {
        markAsSent(this.recipients.stream().map(Recipient::sent).toList());
    }

    public void markAsSent(List<Recipient> recipients) {
        this.status = SmsStatus.SENT;
        this.sentAt = Instant.now();
        this.confirmedAt = this.sentAt;
        this.recipients = recipients;
        this.successCount = recipients.size();
        this.failCount = 0;
    }

    public void markAsFailed() {
        markAsFailed(this.recipients.stream().map(recipient -> recipient.failed(null)).toList());
    }

    public void markAsFailed(List<Recipient> recipients) {
        this.status = SmsStatus.FAILED;
        this.sentAt = Instant.now();
        this.confirmedAt = this.sentAt;
        this.recipients = recipients;
        this.successCount = 0;
        this.failCount = recipients.size();
    }

    public void markAsPartial(int successCount, int failCount) {
        markAsPartial(this.recipients, successCount, failCount);
    }

    public void markAsPartial(List<Recipient> recipients, int successCount, int failCount) {
        this.status = SmsStatus.PARTIAL;
        this.sentAt = Instant.now();
        this.confirmedAt = this.sentAt;
        this.recipients = recipients;
        this.successCount = successCount;
        this.failCount = failCount;
    }

    public void cancel() {
        this.status = SmsStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    public boolean isPending() {
        return this.status == SmsStatus.PENDING;
    }

    public void recordDispatchAttempt(String provider) {
        this.provider = provider;
        this.dispatchAttemptCount++;
        this.lastProviderError = null;
    }

    public void markAsDispatched(String provider, String providerGroupId, List<Recipient> recipients,
                                 String providerStatus) {
        this.status = SmsStatus.DISPATCHED;
        this.provider = provider;
        this.providerGroupId = providerGroupId;
        this.dispatchedAt = Instant.now();
        this.recipients = recipients;
        this.lastProviderStatus = providerStatus;
        this.lastProviderError = null;
    }

    public void recordRetryableDispatchFailure(String provider, String errorMessage) {
        this.provider = provider;
        this.lastProviderStatus = "FAILED_RETRYABLE";
        this.lastProviderError = errorMessage;
    }

    public void recordTerminalDispatchFailure(String provider, String errorMessage) {
        this.provider = provider;
        this.lastProviderStatus = "FAILED";
        this.lastProviderError = errorMessage;
        markAsFailed(this.recipients.stream()
                .map(recipient -> recipient.failed(errorMessage))
                .toList());
    }

    @Converter
    static class RecipientsConverter implements AttributeConverter<List<Recipient>, String> {

        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(List<Recipient> attribute) {
            if (attribute == null) {
                return null;
            }
            try {
                return OBJECT_MAPPER.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new BusinessException(SmsErrorCode.JSON_SERIALIZATION_FAILED, e);
            }
        }

        @Override
        public List<Recipient> convertToEntityAttribute(String dbData) {
            if (dbData == null) {
                return null;
            }
            try {
                return OBJECT_MAPPER.readValue(dbData, new TypeReference<List<Recipient>>() {});
            } catch (JsonProcessingException e) {
                throw new BusinessException(SmsErrorCode.JSON_SERIALIZATION_FAILED, e);
            }
        }
    }
}
