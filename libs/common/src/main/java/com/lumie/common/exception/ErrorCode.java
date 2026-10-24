package com.lumie.common.exception;

public interface ErrorCode {
    String getCode();

    /** Korean user-facing message for this stable error code. */
    String getMessage();

    int getStatus();
}
