package com.lumie.admin.domain.exception;

import com.lumie.common.exception.BusinessException;

public class AdminNotFoundException extends BusinessException {
    public AdminNotFoundException(Long id) {
        super(AdminErrorCode.ADMIN_NOT_FOUND,
              String.format("Admin not found with id: %d", id));
    }

    public AdminNotFoundException(String identifier) {
        super(AdminErrorCode.ADMIN_NOT_FOUND,
              String.format("Admin not found with identifier: %s", identifier));
    }
}
