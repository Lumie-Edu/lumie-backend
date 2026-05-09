package com.lumie.admin.domain.exception;

import com.lumie.common.exception.BusinessException;

public class DuplicatePhoneException extends BusinessException {

    public DuplicatePhoneException(String phone) {
        super(AdminErrorCode.DUPLICATE_PHONE,
              String.format("Phone number already exists: %s", phone));
    }
}
