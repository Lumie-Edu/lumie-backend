package com.lumie.notification.domain.exception;

import com.lumie.common.exception.BusinessException;

public class SmsMessageNotFoundException extends BusinessException {

    public SmsMessageNotFoundException(Long id) {
        super(SmsErrorCode.MESSAGE_NOT_FOUND, "SMS message not found with id: " + id);
    }
}
