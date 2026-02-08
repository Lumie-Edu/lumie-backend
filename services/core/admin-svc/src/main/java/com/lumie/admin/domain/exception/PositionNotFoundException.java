package com.lumie.admin.domain.exception;

import com.lumie.common.exception.BusinessException;

public class PositionNotFoundException extends BusinessException {
    public PositionNotFoundException(Long id) {
        super(AdminErrorCode.POSITION_NOT_FOUND,
              String.format("Position not found with id: %d", id));
    }
}
