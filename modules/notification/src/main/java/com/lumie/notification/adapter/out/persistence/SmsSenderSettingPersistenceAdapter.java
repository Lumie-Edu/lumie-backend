package com.lumie.notification.adapter.out.persistence;

import com.lumie.notification.application.port.out.SmsSenderSettingPersistencePort;
import com.lumie.notification.domain.entity.SmsSenderSetting;
import com.lumie.notification.domain.vo.SmsSenderSettingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SmsSenderSettingPersistenceAdapter implements SmsSenderSettingPersistencePort {

    private final SmsSenderSettingJpaRepository repository;

    @Override
    public Optional<SmsSenderSetting> findByProvider(String provider) {
        return repository.findByProvider(provider);
    }

    @Override
    public Optional<SmsSenderSetting> findByProviderAndRegistrationStatus(String provider, SmsSenderSettingStatus registrationStatus) {
        return repository.findByProviderAndRegistrationStatus(provider, registrationStatus);
    }

    @Override
    public SmsSenderSetting save(SmsSenderSetting setting) {
        return repository.save(setting);
    }
}
