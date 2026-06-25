package com.lumie.notification.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.common.exception.BusinessException;
import com.lumie.notification.domain.exception.SmsErrorCode;
import com.lumie.notification.domain.vo.Recipient;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "sms_recipient_groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SmsRecipientGroup extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "owner_staff_id", nullable = false)
    private Long ownerStaffId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "icon", nullable = false, length = 50)
    private String icon;

    @Convert(converter = RecipientsConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recipients", nullable = false, columnDefinition = "jsonb")
    private List<Recipient> recipients;

    @Builder
    private SmsRecipientGroup(Long ownerStaffId, String name, String icon, List<Recipient> recipients) {
        this.ownerStaffId = ownerStaffId;
        this.name = name;
        this.icon = icon == null || icon.isBlank() ? "mic" : icon;
        this.recipients = List.copyOf(recipients);
    }

    public static SmsRecipientGroup create(Long ownerStaffId, String name, String icon, List<Recipient> recipients) {
        return SmsRecipientGroup.builder()
                .ownerStaffId(ownerStaffId)
                .name(name)
                .icon(icon)
                .recipients(recipients)
                .build();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SmsRecipientGroup that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
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
                return List.of();
            }
            try {
                return OBJECT_MAPPER.readValue(dbData, new TypeReference<List<Recipient>>() {});
            } catch (JsonProcessingException e) {
                throw new BusinessException(SmsErrorCode.JSON_SERIALIZATION_FAILED, e);
            }
        }
    }
}
