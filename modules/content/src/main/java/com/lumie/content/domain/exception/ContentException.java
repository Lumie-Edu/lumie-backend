package com.lumie.content.domain.exception;

import com.lumie.common.exception.BusinessException;

public class ContentException extends BusinessException {

    public ContentException(ContentErrorCode errorCode) {
        super(errorCode);
    }

    public ContentException(ContentErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
