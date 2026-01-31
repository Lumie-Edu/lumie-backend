package com.lumie.academy.domain.exception;

import com.lumie.common.exception.BusinessException;

public class AdminNotFoundException extends BusinessException {

    public AdminNotFoundException(Long id) {
        super(AcademyErrorCode.ADMIN_NOT_FOUND,
              String.format("Admin not found with id: %d", id));
    }

    public AdminNotFoundException(String email) {
        super(AcademyErrorCode.ADMIN_NOT_FOUND,
              String.format("Admin not found with email: %s", email));
    }
}
