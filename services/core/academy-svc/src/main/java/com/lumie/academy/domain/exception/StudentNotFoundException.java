package com.lumie.academy.domain.exception;

import com.lumie.common.exception.BusinessException;

public class StudentNotFoundException extends BusinessException {

    public StudentNotFoundException(Long id) {
        super(AcademyErrorCode.STUDENT_NOT_FOUND,
              String.format("Student not found with id: %d", id));
    }

    public StudentNotFoundException(String email) {
        super(AcademyErrorCode.STUDENT_NOT_FOUND,
              String.format("Student not found with email: %s", email));
    }
}
