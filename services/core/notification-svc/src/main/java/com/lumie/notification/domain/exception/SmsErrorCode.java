package com.lumie.notification.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SmsErrorCode implements ErrorCode {

    MESSAGE_NOT_FOUND("SMS_001", "SMS message not found", 404),
    TEMPLATE_NOT_FOUND("SMS_002", "SMS template not found", 404),
    EMPTY_RECIPIENTS("SMS_003", "Recipients list cannot be empty", 400),
    INVALID_STATUS("SMS_004", "Invalid SMS status value", 400),
    ACADEMY_NOT_FOUND("SMS_005", "Academy not found or inactive", 404);

    private final String code;
    private final String message;
    private final int status;
}
