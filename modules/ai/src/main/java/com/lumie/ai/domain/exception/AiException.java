package com.lumie.ai.domain.exception;

import com.lumie.common.exception.BusinessException;

public class AiException extends BusinessException {

    public AiException(AiErrorCode errorCode) {
        super(errorCode);
    }

    public AiException(AiErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public AiException(AiErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
