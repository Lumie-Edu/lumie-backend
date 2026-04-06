package com.lumie.cls.domain.exception;

import com.lumie.common.exception.BusinessException;

public class ClassFullException extends BusinessException {

    public ClassFullException(Long classId) {
        super(ClassErrorCode.CLASS_FULL, "Class has reached maximum student capacity: " + classId);
    }
}
