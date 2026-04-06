package com.lumie.cls.domain.exception;

import com.lumie.common.exception.BusinessException;

public class EnrollmentNotFoundException extends BusinessException {

    public EnrollmentNotFoundException(Long id) {
        super(ClassErrorCode.ENROLLMENT_NOT_FOUND, "Enrollment not found with id: " + id);
    }
}
