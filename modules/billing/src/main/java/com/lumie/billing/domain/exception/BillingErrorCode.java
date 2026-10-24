package com.lumie.billing.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BillingErrorCode implements ErrorCode {

    // Plan errors
    PLAN_NOT_FOUND("BILLING_001", "요금제를 찾을 수 없습니다.", 404),

    // Subscription errors
    SUBSCRIPTION_NOT_FOUND("BILLING_101", "구독 정보를 찾을 수 없습니다.", 404),
    SUBSCRIPTION_ALREADY_EXISTS("BILLING_102", "이미 구독 중인 학원입니다.", 409),
    INVALID_SUBSCRIPTION_STATE("BILLING_103", "현재 구독 상태에서는 요청을 처리할 수 없습니다.", 400),
    SUBSCRIPTION_PAUSED("BILLING_104", "구독이 일시 중지된 상태입니다.", 402),

    // Quota errors
    QUOTA_EXCEEDED("BILLING_201", "사용 한도를 초과했습니다.", 403),

    // Invoice errors
    INVOICE_NOT_FOUND("BILLING_301", "청구서를 찾을 수 없습니다.", 404),
    INVALID_INVOICE_STATE("BILLING_302", "현재 청구서 상태에서는 요청을 처리할 수 없습니다.", 400),
    PAYMENT_AMOUNT_MISMATCH("BILLING_303", "결제 금액이 청구 금액과 일치하지 않습니다.", 400),

    // Payment errors
    PAYMENT_FAILED("BILLING_401", "결제에 실패했습니다. 결제 수단을 확인해 주세요.", 402),
    PAYMENT_RETRY_EXHAUSTED("BILLING_402", "결제 재시도 횟수를 초과해 구독이 일시 중지되었습니다.", 402),

    // BillingKey errors
    BILLING_KEY_NOT_FOUND("BILLING_501", "등록된 결제 수단을 찾을 수 없습니다.", 404),
    BILLING_KEY_REVOKED("BILLING_502", "결제 수단이 해지되어 사용할 수 없습니다.", 400),
    BILLING_KEY_ALREADY_ACTIVE("BILLING_503", "이미 등록된 결제 수단이 있습니다.", 409),

    // AlimtalkCredit errors
    ALIMTALK_CREDIT_NOT_FOUND("BILLING_601", "알림톡 잔액 정보를 찾을 수 없습니다.", 404),
    ALIMTALK_CREDIT_INSUFFICIENT("BILLING_602", "알림톡 잔액이 부족합니다.", 402),

    // TaxInvoice errors
    TAX_INVOICE_NOT_FOUND("BILLING_701", "세금계산서를 찾을 수 없습니다.", 404),
    TAX_INVOICE_ISSUE_FAILED("BILLING_702", "세금계산서 발행에 실패했습니다.", 502),
    TAX_INVOICE_ALREADY_ISSUED("BILLING_703", "세금계산서가 이미 발행되었습니다.", 409),

    // Webhook errors
    TOSS_WEBHOOK_SIGNATURE_INVALID("BILLING_801", "결제 요청을 확인할 수 없습니다.", 401),
    TOSS_API_ERROR("BILLING_802", "결제 요청을 처리하지 못했습니다.", 502),

    // Operation lock errors
    OPERATION_IN_PROGRESS("BILLING_901", "다른 결제 작업이 진행 중입니다.", 409),

    // General errors
    INTERNAL_ERROR("BILLING_999", "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.", 500);

    private final String code;
    private final String message;
    private final int status;
}
