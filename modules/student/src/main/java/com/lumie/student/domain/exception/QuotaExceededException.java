package com.lumie.student.domain.exception;

import com.lumie.common.exception.BusinessException;

public class QuotaExceededException extends BusinessException {

    public QuotaExceededException(String metricType, long current, long limit) {
        super(StudentErrorCode.QUOTA_EXCEEDED,
              String.format("Quota exceeded for %s: current=%d, limit=%d", metricType, current, limit));
    }
}
