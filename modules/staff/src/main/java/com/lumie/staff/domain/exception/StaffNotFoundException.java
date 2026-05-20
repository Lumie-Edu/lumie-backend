package com.lumie.staff.domain.exception;

import com.lumie.common.exception.BusinessException;

public class StaffNotFoundException extends BusinessException {
    public StaffNotFoundException(Long id) {
        super(StaffErrorCode.STAFF_NOT_FOUND,
              String.format("Staff not found with id: %d", id));
    }

    public StaffNotFoundException(String identifier) {
        super(StaffErrorCode.STAFF_NOT_FOUND,
              String.format("Staff not found with identifier: %s", identifier));
    }
}
