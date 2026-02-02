package com.lumie.content.application.service;

import com.lumie.content.application.dto.request.UpdateReviewPopupSettingRequest;
import com.lumie.content.application.dto.response.ReviewPopupSettingResponse;
import com.lumie.content.domain.entity.ReviewPopupSetting;
import com.lumie.content.domain.repository.ReviewPopupSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewPopupSettingService {

    private final ReviewPopupSettingRepository settingRepository;

    @Transactional(readOnly = true)
    public ReviewPopupSettingResponse getPopupSetting() {
        log.debug("Getting review popup setting");

        ReviewPopupSetting setting = settingRepository.findFirst()
                .orElseGet(() -> {
                    log.info("No popup setting found, creating default");
                    return settingRepository.save(new ReviewPopupSetting(1L, true));
                });

        return ReviewPopupSettingResponse.from(setting);
    }

    @Transactional
    public ReviewPopupSettingResponse updatePopupSetting(UpdateReviewPopupSettingRequest request) {
        log.info("Updating review popup setting: isReviewPopupOn={}", request.isReviewPopupOn());

        ReviewPopupSetting setting = settingRepository.findFirst()
                .orElseGet(() -> new ReviewPopupSetting(1L, request.isReviewPopupOn()));

        setting.updateIsReviewPopupOn(request.isReviewPopupOn());

        ReviewPopupSetting saved = settingRepository.save(setting);
        log.info("Review popup setting updated: id={}", saved.getId());

        return ReviewPopupSettingResponse.from(saved);
    }
}
