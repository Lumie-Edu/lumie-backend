package com.lumie.content.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateReviewPopupSettingRequest(
        @NotNull(message = "isReviewPopupOn is required")
        Boolean isReviewPopupOn
) {
}
