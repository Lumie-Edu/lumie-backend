package com.lumie.academy.domain.exception;

import com.lumie.common.exception.BusinessException;

public class DuplicateUserLoginIdException extends BusinessException {

    public DuplicateUserLoginIdException(String userLoginId) {
        super(AcademyErrorCode.DUPLICATE_USER_LOGIN_ID,
              String.format("User login ID already exists: %s", userLoginId));
    }
}
