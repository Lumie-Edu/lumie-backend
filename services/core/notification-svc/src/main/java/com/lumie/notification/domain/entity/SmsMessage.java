package com.lumie.notification.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.common.domain.BaseEntity;
import com.lumie.notification.domain.vo.Recipient;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sms_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SmsMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 20)
    private RecipientType recipientType;

    @Convert(converter = RecipientsConverter.class)
    @Column(name = "recipients", nullable = false, columnDefinition = "jsonb")
    private List<Recipient> recipients;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SmsStatus status;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Builder
    private SmsMessage(Long academyId, Long senderId, RecipientType recipientType,
                       List<Recipient> recipients, String title, String content,
                       Long templateId, SmsStatus status) {
        this.academyId = academyId;
        this.senderId = senderId;
        this.recipientType = recipientType;
        this.recipients = recipients;
        this.title = title;
        this.content = content;
        this.templateId = templateId;
        this.status = status != null ? status : SmsStatus.PENDING;
        this.successCount = 0;
        this.failCount = 0;
    }

    public static SmsMessage create(Long academyId, Long senderId, RecipientType recipientType,
                                    List<Recipient> recipients, String title, String content,
                                    Long templateId) {
        return SmsMessage.builder()
                .academyId(academyId)
                .senderId(senderId)
                .recipientType(recipientType)
                .recipients(recipients)
                .title(title)
                .content(content)
                .templateId(templateId)
                .status(SmsStatus.PENDING)
                .build();
    }

    public void markAsSent() {
        this.status = SmsStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.successCount = this.recipients.size();
        this.failCount = 0;
    }

    public void markAsFailed() {
        this.status = SmsStatus.FAILED;
        this.sentAt = LocalDateTime.now();
        this.successCount = 0;
        this.failCount = this.recipients.size();
    }

    public void markAsPartial(int successCount, int failCount) {
        this.status = SmsStatus.PARTIAL;
        this.sentAt = LocalDateTime.now();
        this.successCount = successCount;
        this.failCount = failCount;
    }

    public boolean isPending() {
        return this.status == SmsStatus.PENDING;
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
                throw new IllegalArgumentException("Failed to convert Recipients to JSON", e);
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
                throw new IllegalArgumentException("Failed to convert JSON to Recipients", e);
            }
        }
    }
}
