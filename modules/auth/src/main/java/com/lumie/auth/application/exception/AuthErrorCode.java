package com.lumie.auth.application.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_CREDENTIALS("AUTH_001", "아이디 또는 비밀번호가 올바르지 않습니다.", 401),
    USER_DISABLED("AUTH_004", "사용할 수 없는 계정입니다.", 403),
    TENANT_INACTIVE("AUTH_005", "이용이 중지된 학원입니다.", 403),
    SESSION_PERSISTENCE_FAILED("AUTH_008", "로그인 정보를 저장하지 못했습니다.", 500),
    LOGIN_ID_ALREADY_EXISTS("AUTH_009", "이미 사용 중인 아이디입니다.", 409),
    TENANT_CREATION_FAILED("AUTH_010", "학원 등록을 완료하지 못했습니다.", 500),
    LOGIN_ID_GENERATION_FAILED("AUTH_011", "사용 가능한 아이디를 만들지 못했습니다.", 500),
    CURRENT_PASSWORD_INCORRECT("AUTH_015", "현재 비밀번호가 올바르지 않습니다.", 422),
    PASSWORD_UNCHANGED("AUTH_016", "새 비밀번호는 현재 비밀번호와 달라야 합니다.", 422);

    private final String code;
    private final String message;
    private final int status;
}
