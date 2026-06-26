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
    SENDER_NOT_FOUND("SMS_005", "Sender not found", 404),
    CLASS_NOT_FOUND("SMS_006", "Class not found", 404),
    JSON_SERIALIZATION_FAILED("SMS_007", "Failed to serialize or deserialize JSON field", 500),
    MESSAGE_NOT_PENDING("SMS_008", "SMS message is not pending", 400),
    INVALID_SCHEDULED_AT("SMS_009", "Scheduled time must be in the future", 400),
    INVALID_RECIPIENTS("SMS_010", "Recipients contain invalid recipient data", 400),
    RECIPIENT_GROUP_NOT_FOUND("SMS_011", "SMS recipient group not found", 404),
    INVALID_SEND_TARGET("SMS_012", "Provide either recipients or recipient group for SMS sending", 400);

    private final String code;
    private final String message;
    private final int status;
}
