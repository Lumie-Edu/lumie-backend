package com.lumie.classroom.domain.exception;

import com.lumie.common.exception.BusinessException;

public class ClassNotFoundException extends BusinessException {

    public ClassNotFoundException(Long id) {
        super(ClassErrorCode.CLASS_NOT_FOUND, "Class not found with id: " + id);
    }
}
