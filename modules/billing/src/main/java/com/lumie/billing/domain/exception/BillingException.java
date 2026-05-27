package com.lumie.billing.domain.exception;

import com.lumie.common.exception.BusinessException;

public class BillingException extends BusinessException {

    public BillingException(BillingErrorCode errorCode) {
        super(errorCode);
    }

    public BillingException(BillingErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BillingException(BillingErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
