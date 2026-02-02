package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.ReviewPopupSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaReviewPopupSettingRepository extends JpaRepository<ReviewPopupSetting, Long> {

    Optional<ReviewPopupSetting> findFirstByOrderByIdAsc();
}
