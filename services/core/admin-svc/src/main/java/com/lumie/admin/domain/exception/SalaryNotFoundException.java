package com.lumie.admin.domain.exception;

import com.lumie.common.exception.BusinessException;

public class SalaryNotFoundException extends BusinessException {
    public SalaryNotFoundException(Long adminId) {
        super(AdminErrorCode.CURRENT_SALARY_NOT_FOUND,
              String.format("No current effective salary found for admin: %d", adminId));
    }
}
