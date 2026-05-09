package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.ReviewPopupSetting;

import java.time.LocalDateTime;

public record ReviewPopupSettingResponse(
        Long id,
        Boolean isReviewPopupOn,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ReviewPopupSettingResponse from(ReviewPopupSetting setting) {
        return new ReviewPopupSettingResponse(
                setting.getId(),
                setting.getIsReviewPopupOn(),
                setting.getCreatedAt(),
                setting.getUpdatedAt()
        );
    }
}
