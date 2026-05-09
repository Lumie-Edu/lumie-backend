package com.lumie.tenant.infrastructure.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TenantErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_SLUG("T001", "Invalid tenant slug format", 400),
    RESERVED_SLUG("T002", "Slug is reserved and cannot be used", 400),

    // 404 Not Found
    TENANT_NOT_FOUND("T003", "Tenant not found", 404),

    // 409 Conflict
    SLUG_ALREADY_EXISTS("T004", "Tenant with this slug already exists", 409),
    INVALID_STATUS_TRANSITION("T005", "Invalid tenant status transition", 409),

    // 500 Internal Server Error
    SCHEMA_PROVISIONING_FAILED("T006", "Failed to provision tenant schema", 500);

    private final String code;
    private final String message;
    private final int status;
}
