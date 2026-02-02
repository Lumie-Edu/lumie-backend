package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.ReviewPopupSetting;
import com.lumie.content.domain.repository.ReviewPopupSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewPopupSettingRepositoryAdapter implements ReviewPopupSettingRepository {

    private final JpaReviewPopupSettingRepository jpaRepository;

    @Override
    public ReviewPopupSetting save(ReviewPopupSetting setting) {
        return jpaRepository.save(setting);
    }

    @Override
    public Optional<ReviewPopupSetting> findFirst() {
        return jpaRepository.findFirstByOrderByIdAsc();
    }
}
