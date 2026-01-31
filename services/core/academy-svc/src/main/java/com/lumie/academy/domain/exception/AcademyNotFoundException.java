package com.lumie.academy.domain.exception;

import com.lumie.common.exception.BusinessException;

public class AcademyNotFoundException extends BusinessException {

    public AcademyNotFoundException(Long id) {
        super(AcademyErrorCode.ACADEMY_NOT_FOUND,
              String.format("Academy not found with id: %d", id));
    }

    public AcademyNotFoundException(String name) {
        super(AcademyErrorCode.ACADEMY_NOT_FOUND,
              String.format("Academy not found with name: %s", name));
    }
}
