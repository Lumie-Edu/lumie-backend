package com.lumie.admin.domain.exception;

import com.lumie.common.exception.BusinessException;

public class PayrollNotFoundException extends BusinessException {
    public PayrollNotFoundException(Long id) {
        super(AdminErrorCode.PAYROLL_NOT_FOUND,
              String.format("Payroll record not found with id: %d", id));
    }
}
