package com.lumie.notification.adapter.out.persistence;

import com.lumie.notification.domain.entity.SmsSenderSetting;
import com.lumie.notification.domain.vo.SmsSenderSettingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SmsSenderSettingJpaRepository extends JpaRepository<SmsSenderSetting, Long> {

    Optional<SmsSenderSetting> findByProvider(String provider);

    Optional<SmsSenderSetting> findByProviderAndRegistrationStatus(String provider, SmsSenderSettingStatus registrationStatus);
}
