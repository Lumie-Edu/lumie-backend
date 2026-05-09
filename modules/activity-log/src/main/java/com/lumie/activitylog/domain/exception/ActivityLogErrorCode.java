package com.lumie.activitylog.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityLogErrorCode implements ErrorCode {

    // 403 Forbidden
    INSUFFICIENT_ROLE("ACTIVITY_LOG_001", "활동 로그를 조회할 권한이 없습니다.", 403);

    private final String code;
    private final String message;
    private final int status;
}
