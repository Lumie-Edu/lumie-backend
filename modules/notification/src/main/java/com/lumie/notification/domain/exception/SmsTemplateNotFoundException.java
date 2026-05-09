package com.lumie.notification.domain.exception;

import com.lumie.common.exception.BusinessException;

public class SmsTemplateNotFoundException extends BusinessException {

    public SmsTemplateNotFoundException(Long id) {
        super(SmsErrorCode.TEMPLATE_NOT_FOUND, "SMS template not found with id: " + id);
    }
}
