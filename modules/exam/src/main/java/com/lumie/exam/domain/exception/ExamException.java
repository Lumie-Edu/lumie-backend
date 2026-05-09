package com.lumie.exam.domain.exception;

import com.lumie.common.exception.BusinessException;

public class ExamException extends BusinessException {

    public ExamException(ExamErrorCode errorCode) {
        super(errorCode);
    }

    public ExamException(ExamErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
