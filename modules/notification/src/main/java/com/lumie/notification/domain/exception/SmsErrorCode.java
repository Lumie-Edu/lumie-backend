package com.lumie.notification.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SmsErrorCode implements ErrorCode {

    MESSAGE_NOT_FOUND("SMS_001", "문자 메시지를 찾을 수 없습니다.", 404),
    TEMPLATE_NOT_FOUND("SMS_002", "문자 템플릿을 찾을 수 없습니다.", 404),
    EMPTY_RECIPIENTS("SMS_003", "수신자를 한 명 이상 선택해 주세요.", 400),
    INVALID_STATUS("SMS_004", "문자 발송 상태값이 올바르지 않습니다.", 400),
    SENDER_NOT_FOUND("SMS_005", "발신 정보를 찾을 수 없습니다.", 404),
    CLASS_NOT_FOUND("SMS_006", "수업을 찾을 수 없습니다.", 404),
    JSON_SERIALIZATION_FAILED("SMS_007", "요청 정보를 처리하지 못했습니다.", 500),
    MESSAGE_NOT_PENDING("SMS_008", "대기 중인 문자만 처리할 수 있습니다.", 400),
    INVALID_SCHEDULED_AT("SMS_009", "예약 시간은 현재 이후로 설정해 주세요.", 400),
    INVALID_RECIPIENTS("SMS_010", "문자 수신자 정보가 올바르지 않습니다.", 400),
    RECIPIENT_GROUP_NOT_FOUND("SMS_011", "문자 수신 그룹을 찾을 수 없습니다.", 404),
    INVALID_SEND_TARGET("SMS_012", "문자 수신자 또는 수신 그룹을 선택해 주세요.", 400),
    PROVIDER_CONFIGURATION_INVALID("SMS_013", "문자 발송 설정이 올바르지 않습니다.", 500),
    PROVIDER_SEND_FAILED("SMS_014", "문자 발송에 실패했습니다.", 502),
    WEBHOOK_UNAUTHORIZED("SMS_015", "문자 발송 요청을 확인할 수 없습니다.", 401),
    WEBHOOK_IGNORED("SMS_016", "문자 발송 결과를 처리하지 못했습니다.", 202),
    SENDER_SETTING_NOT_FOUND("SMS_017", "문자 발신번호 설정을 찾을 수 없습니다.", 404),
    SENDER_SETTING_NOT_APPROVED("SMS_018", "문자 발신번호 승인이 완료되지 않았습니다.", 400),
    INVALID_SENDER_NUMBER("SMS_019", "문자 발신번호가 올바르지 않습니다.", 400),
    INVALID_SENDER_SETTING_STATUS("SMS_020", "문자 발신번호 상태가 올바르지 않습니다.", 400);

    private final String code;
    private final String message;
    private final int status;
}
