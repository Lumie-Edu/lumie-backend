package com.lumie.admin.domain.exception;

import com.lumie.common.exception.BusinessException;

public class WorkScheduleNotFoundException extends BusinessException {
    public WorkScheduleNotFoundException(Long id) {
        super(AdminErrorCode.WORK_SCHEDULE_NOT_FOUND,
              String.format("Work schedule not found with id: %d", id));
    }
}
