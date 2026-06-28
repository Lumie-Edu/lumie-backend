package com.lumie.notification.application.port.out;

import com.lumie.notification.domain.entity.SmsSenderSetting;
import com.lumie.notification.domain.vo.SmsSenderSettingStatus;

import java.util.Optional;

public interface SmsSenderSettingPersistencePort {

    Optional<SmsSenderSetting> findByProvider(String provider);

    Optional<SmsSenderSetting> findByProviderAndRegistrationStatus(String provider, SmsSenderSettingStatus registrationStatus);

    SmsSenderSetting save(SmsSenderSetting setting);
}
