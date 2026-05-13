package com.lumie.common.exception;

public class BusinessRuleViolationException extends BusinessException {

    public BusinessRuleViolationException(String message) {
        super(CommonErrorCode.BUSINESS_RULE_VIOLATION, message);
    }

    public BusinessRuleViolationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
