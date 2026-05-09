package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.ReviewPopupSetting;

import java.util.Optional;

public interface ReviewPopupSettingRepository {

    ReviewPopupSetting save(ReviewPopupSetting setting);

    Optional<ReviewPopupSetting> findFirst();
}
