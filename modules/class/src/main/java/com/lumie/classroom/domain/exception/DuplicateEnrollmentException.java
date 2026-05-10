package com.lumie.classroom.domain.exception;

import com.lumie.common.exception.BusinessException;

public class DuplicateEnrollmentException extends BusinessException {

    public DuplicateEnrollmentException(Long classId, Long studentId) {
        super(ClassErrorCode.DUPLICATE_ENROLLMENT,
              "Student " + studentId + " is already enrolled in class " + classId);
    }
}
