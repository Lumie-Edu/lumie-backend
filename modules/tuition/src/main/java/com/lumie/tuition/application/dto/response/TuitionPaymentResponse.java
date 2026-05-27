package com.lumie.tuition.application.dto.response;

import com.lumie.tuition.domain.entity.TuitionPayment;
import com.lumie.tuition.domain.vo.TuitionPaymentMethod;
import com.lumie.tuition.domain.vo.TuitionPaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TuitionPaymentResponse(
        Long id,
        Long tuitionInvoiceId,
        String orderId,
        String pgTransactionId,
        BigDecimal amount,
        TuitionPaymentMethod method,
        TuitionPaymentStatus status,
        Instant capturedAt,
        Instant failedAt,
        String failureReason,
        BigDecimal refundedAmount,
        String refundReason,
        Instant refundedAt,
        Long refundedByStaffId,
        Instant createdAt,
        Instant updatedAt
) {
    public static TuitionPaymentResponse from(TuitionPayment payment) {
        return new TuitionPaymentResponse(
                payment.getId(),
                payment.getTuitionInvoiceId(),
                payment.getOrderId(),
                payment.getPgTransactionId(),
                payment.getAmount() != null ? payment.getAmount().getAmount() : null,
                payment.getMethod(),
                payment.getStatus(),
                payment.getCapturedAt(),
                payment.getFailedAt(),
                payment.getFailureReason(),
                payment.getRefundedAmount() != null ? payment.getRefundedAmount().getAmount() : null,
                payment.getRefundReason(),
                payment.getRefundedAt(),
                payment.getRefundedByStaffId(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
