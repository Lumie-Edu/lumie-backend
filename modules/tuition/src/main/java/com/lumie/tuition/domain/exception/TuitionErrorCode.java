package com.lumie.tuition.domain.exception;

import com.lumie.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TuitionErrorCode implements ErrorCode {

    // Guardian errors
    GUARDIAN_NOT_FOUND("TUITION_001", "Guardian not found", 404),
    GUARDIAN_ALREADY_LINKED("TUITION_002", "Guardian is already linked to this student", 409),

    // MerchantProfile errors
    MERCHANT_PROFILE_NOT_FOUND("TUITION_101", "Merchant profile not found", 404),
    MERCHANT_PROFILE_ALREADY_EXISTS("TUITION_102", "Merchant profile already exists for this tenant", 409),
    MERCHANT_NOT_APPROVED("TUITION_103", "Merchant is not KYC-approved. Submit KYC first.", 403),
    INVALID_KYC_STATUS("TUITION_104", "Invalid KYC status for this operation", 400),

    // TuitionInvoice errors
    INVOICE_NOT_FOUND("TUITION_201", "Tuition invoice not found", 404),
    INVALID_INVOICE_STATUS("TUITION_202", "Invalid invoice status for this operation", 400),
    IDEMPOTENCY_CONFLICT("TUITION_203", "A tuition invoice with this idempotency key already exists", 409),

    // TuitionPayment errors
    PAYMENT_NOT_FOUND("TUITION_301", "Tuition payment not found", 404),
    INVALID_PAYMENT_STATUS("TUITION_302", "Invalid payment status for this operation", 400),
    REFUND_EXCEEDS_PAYMENT("TUITION_303", "Refund amount exceeds the original payment amount", 400),
    PAYMENT_AMOUNT_MISMATCH("TUITION_304", "Payment amount does not match invoice total", 400),
    ORDER_ID_CONFLICT("TUITION_305", "A payment with this orderId already exists", 409),

    // CashReceipt errors
    CASH_RECEIPT_NOT_FOUND("TUITION_401", "Cash receipt not found", 404),
    INVALID_CASH_RECEIPT_STATUS("TUITION_402", "Invalid cash receipt status for this operation", 400),

    // Toss Payments errors
    TOSS_WEBHOOK_SIGNATURE_INVALID("TUITION_501", "Invalid Toss webhook signature", 401),
    TOSS_API_ERROR("TUITION_502", "Toss Payments API call failed", 502),

    // General errors
    INTERNAL_ERROR("TUITION_999", "Internal server error", 500);

    private final String code;
    private final String message;
    private final int status;
}
