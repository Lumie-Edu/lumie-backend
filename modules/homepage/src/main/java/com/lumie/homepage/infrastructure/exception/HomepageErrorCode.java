package com.lumie.homepage.infrastructure.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HomepageErrorCode implements ErrorCode {

    // 404 Not Found
    HOMEPAGE_CONFIG_NOT_FOUND("HOMEPAGE_001", "Homepage configuration not found", 404),

    // 400 Bad Request
    INVALID_TEMPLATE_ID("HOMEPAGE_002", "Invalid template ID", 400),
    INVALID_TEMPLATE_VERSION("HOMEPAGE_003", "Template version must be positive", 400),

    // 422 Unprocessable Entity
    NOT_READY_TO_PUBLISH("HOMEPAGE_004", "Homepage configuration is not ready to publish: theme or sections is blank", 422);

    private final String code;
    private final String message;
    private final int status;
}
