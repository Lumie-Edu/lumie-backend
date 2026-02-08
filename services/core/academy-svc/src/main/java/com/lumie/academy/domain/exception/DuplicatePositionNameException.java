package com.lumie.academy.domain.exception;

import com.lumie.common.exception.BusinessException;

public class DuplicatePositionNameException extends BusinessException {

    public DuplicatePositionNameException(String name) {
        super(AcademyErrorCode.DUPLICATE_POSITION_NAME,
              String.format("이미 존재하는 직책명입니다: %s", name));
    }
}
