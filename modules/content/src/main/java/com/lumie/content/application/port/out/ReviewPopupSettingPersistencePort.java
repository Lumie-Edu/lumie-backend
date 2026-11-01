package com.lumie.content.application.port.out;

import com.lumie.content.domain.entity.ReviewPopupSetting;
import java.util.Optional;

public interface ReviewPopupSettingPersistencePort {
    Optional<ReviewPopupSetting> findFirst();
    ReviewPopupSetting save(ReviewPopupSetting setting);
}
