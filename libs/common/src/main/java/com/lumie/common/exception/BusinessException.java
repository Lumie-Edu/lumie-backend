package com.lumie.common.exception;

import lombok.Getter;

/**
 * 비즈니스 로직에서 발생하는 예외를 나타내는 기본 클래스.
 * 모든 도메인별 예외는 이 클래스를 상속받아 구현한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
