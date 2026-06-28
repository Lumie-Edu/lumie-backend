package com.lumie.notification.application.dto.response;

import com.lumie.notification.domain.vo.SmsSenderSettingStatus;

public record SmsSenderSettingStatusResponse(
        String provider,
        SmsSenderSettingStatus registrationStatus
) {
    public static SmsSenderSettingStatusResponse of(String provider, SmsSenderSettingStatus registrationStatus) {
        return new SmsSenderSettingStatusResponse(provider, registrationStatus);
    }
}
