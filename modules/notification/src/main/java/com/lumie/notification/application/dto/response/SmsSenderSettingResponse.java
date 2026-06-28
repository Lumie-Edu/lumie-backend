package com.lumie.notification.application.dto.response;

import com.lumie.common.util.PhoneNumberUtils;
import com.lumie.notification.domain.entity.SmsSenderSetting;
import com.lumie.notification.domain.vo.SmsSenderSettingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record SmsSenderSettingResponse(
        @Schema(nullable = true)
        Long id,
        String provider,
        @Schema(nullable = true)
        String senderNumber,
        SmsSenderSettingStatus registrationStatus,
        @Schema(nullable = true) String rejectionReason,
        @Schema(nullable = true) Instant approvedAt,
        @Schema(nullable = true) Instant rejectedAt,
        @Schema(nullable = true)
        Instant createdAt,
        @Schema(nullable = true)
        Instant updatedAt
) {
    public static SmsSenderSettingResponse notConfigured(String provider) {
        return new SmsSenderSettingResponse(
                null,
                provider,
                null,
                SmsSenderSettingStatus.NOT_CONFIGURED,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static SmsSenderSettingResponse from(SmsSenderSetting setting) {
        return new SmsSenderSettingResponse(
                setting.getId(),
                setting.getProvider(),
                PhoneNumberUtils.toHyphenated(setting.getSenderNumber()),
                setting.getRegistrationStatus(),
                setting.getRejectionReason(),
                setting.getApprovedAt(),
                setting.getRejectedAt(),
                setting.getCreatedAt(),
                setting.getUpdatedAt()
        );
    }
}
