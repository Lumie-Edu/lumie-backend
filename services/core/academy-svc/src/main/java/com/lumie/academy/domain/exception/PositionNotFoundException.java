package com.lumie.academy.domain.exception;

import com.lumie.common.exception.BusinessException;

public class PositionNotFoundException extends BusinessException {

    public PositionNotFoundException(Long id) {
        super(AcademyErrorCode.POSITION_NOT_FOUND,
              String.format("Position not found with id: %d", id));
    }

    public PositionNotFoundException(String name) {
        super(AcademyErrorCode.POSITION_NOT_FOUND,
              String.format("Position not found with name: %s", name));
    }
}
