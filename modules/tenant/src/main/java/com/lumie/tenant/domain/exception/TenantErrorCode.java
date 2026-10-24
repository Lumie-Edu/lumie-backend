package com.lumie.tenant.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TenantErrorCode implements ErrorCode {

    // 400 Bad Request
    INVALID_SLUG("TENANT_001", "학원 주소 형식이 올바르지 않습니다.", 400),
    RESERVED_SLUG("TENANT_002", "사용할 수 없는 학원 주소입니다.", 400),

    // 404 Not Found
    TENANT_NOT_FOUND("TENANT_003", "학원 정보를 찾을 수 없습니다.", 404),

    // 409 Conflict
    SLUG_ALREADY_EXISTS("TENANT_004", "이미 사용 중인 학원 주소입니다.", 409),
    INVALID_STATUS_TRANSITION("TENANT_005", "학원 상태를 변경할 수 없습니다.", 409),
    CUSTOM_ID_IMMUTABLE("TENANT_012", "설정한 사용자 지정 ID는 변경할 수 없습니다.", 409),
    INVALID_TENANT_STATE("TENANT_016", "현재 학원 상태에서는 요청을 처리할 수 없습니다.", 409),
    ONBOARDING_ALREADY_COMPLETED("TENANT_018", "학원 등록 절차가 이미 완료되었습니다.", 409),
    TENANT_CONTEXT_MISMATCH("TENANT_019", "요청한 학원 정보가 일치하지 않습니다.", 403),

    // 400 Bad Request
    INVALID_CUSTOM_ID("TENANT_007", "사용자 지정 ID 형식이 올바르지 않습니다.", 400),
    RESERVED_CUSTOM_ID("TENANT_008", "사용할 수 없는 사용자 지정 ID입니다.", 400),
    LOGO_EMPTY("TENANT_009", "로고 파일이 비어 있습니다.", 400),
    LOGO_TOO_LARGE("TENANT_010", "로고 파일은 5MB 이하로 등록해 주세요.", 400),
    LOGO_UNSUPPORTED_CONTENT_TYPE("TENANT_011", "지원하지 않는 로고 파일 형식입니다.", 400),
    INVALID_SIDEBAR_ITEM_ID("TENANT_017", "사이드바 항목 정보가 올바르지 않습니다.", 400),

    // 500 Internal Server Error
    SCHEMA_PROVISIONING_FAILED("TENANT_006", "학원 정보를 준비하지 못했습니다.", 500),
    LOGO_UPLOAD_FAILED("TENANT_013", "학원 로고를 업로드하지 못했습니다.", 500),
    LOGO_DOWNLOAD_FAILED("TENANT_014", "학원 로고를 불러오지 못했습니다.", 500),
    LOGO_READ_FAILED("TENANT_015", "업로드한 로고 파일을 읽지 못했습니다.", 500);

    private final String code;
    private final String message;
    private final int status;
}
