package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.ReviewPopupSetting;

import java.time.Instant;

public record ReviewPopupSettingResponse(
        Long id,
        Boolean isReviewPopupOn,
        Instant createdAt,
        Instant updatedAt
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
