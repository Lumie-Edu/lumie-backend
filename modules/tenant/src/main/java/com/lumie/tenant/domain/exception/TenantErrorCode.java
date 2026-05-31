package com.lumie.tenant.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TenantErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_SLUG("TENANT_001", "Invalid tenant slug format", 400),
    RESERVED_SLUG("TENANT_002", "Slug is reserved and cannot be used", 400),

    // 404 Not Found
    TENANT_NOT_FOUND("TENANT_003", "Tenant not found", 404),

    // 409 Conflict
    SLUG_ALREADY_EXISTS("TENANT_004", "Tenant with this slug already exists", 409),
    INVALID_STATUS_TRANSITION("TENANT_005", "Invalid tenant status transition", 409),
    CUSTOM_ID_IMMUTABLE("TENANT_012", "Custom ID cannot be changed once set", 409),
    INVALID_TENANT_STATE("TENANT_016", "Invalid tenant state: current={0}, required={1}", 409),

    // 400 Bad Request
    INVALID_CUSTOM_ID("TENANT_007", "Invalid custom ID format", 400),
    RESERVED_CUSTOM_ID("TENANT_008", "Custom ID is reserved and cannot be used", 400),
    LOGO_EMPTY("TENANT_009", "Logo file is empty", 400),
    LOGO_TOO_LARGE("TENANT_010", "Logo file exceeds the 5MB size limit", 400),
    LOGO_UNSUPPORTED_CONTENT_TYPE("TENANT_011", "Unsupported logo content type", 400),

    // 500 Internal Server Error
    SCHEMA_PROVISIONING_FAILED("TENANT_006", "Failed to provision tenant schema", 500),
    LOGO_UPLOAD_FAILED("TENANT_013", "Failed to upload tenant logo", 500),
    LOGO_DOWNLOAD_FAILED("TENANT_014", "Failed to download tenant logo", 500),
    LOGO_READ_FAILED("TENANT_015", "Failed to read the uploaded logo file", 500);

    private final String code;
    private final String message;
    private final int status;
}
