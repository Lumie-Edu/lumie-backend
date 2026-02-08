package com.lumie.admin.domain.exception;

import com.lumie.common.exception.BusinessException;

public class DuplicatePositionNameException extends BusinessException {
    public DuplicatePositionNameException(String name) {
        super(AdminErrorCode.DUPLICATE_POSITION_NAME,
              String.format("이미 존재하는 직책명입니다: %s", name));
    }
}
