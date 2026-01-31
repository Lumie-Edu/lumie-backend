package com.lumie.academy.domain.exception;

import com.lumie.common.exception.BusinessException;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException(String email) {
        super(AcademyErrorCode.DUPLICATE_EMAIL,
              String.format("Email already exists: %s", email));
    }
}
