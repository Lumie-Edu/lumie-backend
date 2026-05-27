package com.lumie.tuition.domain.exception;

import com.lumie.common.exception.BusinessException;

public class TuitionException extends BusinessException {

    public TuitionException(TuitionErrorCode errorCode) {
        super(errorCode);
    }

    public TuitionException(TuitionErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public TuitionException(TuitionErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
