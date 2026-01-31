package com.lumie.file.domain.exception;

import com.lumie.common.exception.BusinessException;

public class FileException extends BusinessException {

    public FileException(FileErrorCode errorCode) {
        super(errorCode);
    }

    public FileException(FileErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
