package com.lumie.staff.domain.exception;

import com.lumie.common.exception.BusinessException;

public class PermissionNotFoundException extends BusinessException {
    public PermissionNotFoundException(Long id) {
        super(StaffErrorCode.PERMISSION_NOT_FOUND,
              String.format("Permission not found with id: %d", id));
    }
}
