package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.ReviewPopupSetting;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewPopupSettingRepository extends JpaRepository<ReviewPopupSetting, Long> {

    default Optional<ReviewPopupSetting> findFirst() {
        List<ReviewPopupSetting> page = findAll(PageRequest.of(0, 1)).getContent();
        return page.isEmpty() ? Optional.empty() : Optional.of(page.get(0));
    }
}
