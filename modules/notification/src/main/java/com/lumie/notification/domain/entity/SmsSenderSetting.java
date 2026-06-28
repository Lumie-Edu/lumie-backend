package com.lumie.notification.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.util.PhoneNumberUtils;
import com.lumie.notification.domain.exception.SmsErrorCode;
import com.lumie.notification.domain.vo.SmsSenderSettingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
        name = "sms_sender_settings",
        uniqueConstraints = @UniqueConstraint(name = "uq_sms_sender_settings_tenant_provider", columnNames = {"tenant_id", "provider"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SmsSenderSetting extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "sender_number", nullable = false, length = 20)
    private String senderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 30)
    private SmsSenderSettingStatus registrationStatus;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Builder
    private SmsSenderSetting(String provider, String senderNumber,
                             SmsSenderSettingStatus registrationStatus,
                             String rejectionReason, Instant approvedAt, Instant rejectedAt) {
        this.provider = requireProvider(provider);
        this.senderNumber = normalizeSenderNumber(senderNumber);
        this.registrationStatus = registrationStatus != null ? registrationStatus : SmsSenderSettingStatus.PENDING;
        this.rejectionReason = rejectionReason;
        this.approvedAt = approvedAt;
        this.rejectedAt = rejectedAt;
    }

    public static SmsSenderSetting create(String provider, String senderNumber) {
        return SmsSenderSetting.builder()
                .provider(provider)
                .senderNumber(senderNumber)
                .registrationStatus(SmsSenderSettingStatus.PENDING)
                .build();
    }

    public void submit(String senderNumber) {
        String normalized = normalizeSenderNumber(senderNumber);
        if (Objects.equals(this.senderNumber, normalized)
                && this.registrationStatus == SmsSenderSettingStatus.APPROVED) {
            return;
        }
        this.senderNumber = normalized;
        this.registrationStatus = SmsSenderSettingStatus.PENDING;
        this.rejectionReason = null;
        this.approvedAt = null;
        this.rejectedAt = null;
    }

    public void approve() {
        this.registrationStatus = SmsSenderSettingStatus.APPROVED;
        this.rejectionReason = null;
        this.approvedAt = Instant.now();
        this.rejectedAt = null;
    }

    public void reject(String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new BusinessException(SmsErrorCode.INVALID_SENDER_SETTING_STATUS, "Rejection reason is required");
        }
        this.registrationStatus = SmsSenderSettingStatus.REJECTED;
        this.rejectionReason = rejectionReason.trim();
        this.approvedAt = null;
        this.rejectedAt = Instant.now();
    }

    public boolean isApproved() {
        return registrationStatus == SmsSenderSettingStatus.APPROVED;
    }

    private static String requireProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new BusinessException(SmsErrorCode.INVALID_SENDER_SETTING_STATUS, "Provider is required");
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeSenderNumber(String senderNumber) {
        if (!PhoneNumberUtils.isValid(senderNumber)) {
            throw new BusinessException(SmsErrorCode.INVALID_SENDER_NUMBER, "Valid sender number is required");
        }
        return PhoneNumberUtils.toDigits(senderNumber);
    }
}
