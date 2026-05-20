package com.lumie.staff.domain.exception;

import com.lumie.common.exception.BusinessException;

public class DuplicatePhoneException extends BusinessException {

    public DuplicatePhoneException(String phone) {
        super(StaffErrorCode.DUPLICATE_PHONE,
              String.format("Phone number already exists: %s", phone));
    }
}
