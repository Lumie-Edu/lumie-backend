package com.lumie.tuition.application.dto.response;

import com.lumie.tuition.domain.entity.CashReceipt;
import com.lumie.tuition.domain.vo.CashReceiptIssueStatus;
import com.lumie.tuition.domain.vo.CashReceiptType;

import java.math.BigDecimal;
import java.time.Instant;

public record CashReceiptResponse(
        Long id,
        Long tuitionPaymentId,
        CashReceiptType receiptType,
        String identifier,
        BigDecimal amount,
        BigDecimal vatAmount,
        String approvalNumber,
        CashReceiptIssueStatus issueStatus,
        Instant issuedAt,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static CashReceiptResponse from(CashReceipt receipt) {
        return new CashReceiptResponse(
                receipt.getId(),
                receipt.getTuitionPaymentId(),
                receipt.getReceiptType(),
                receipt.getIdentifier(),
                receipt.getAmount() != null ? receipt.getAmount().getAmount() : null,
                receipt.getVatAmount() != null ? receipt.getVatAmount().getAmount() : null,
                receipt.getApprovalNumber(),
                receipt.getIssueStatus(),
                receipt.getIssuedAt(),
                receipt.getFailureReason(),
                receipt.getCreatedAt(),
                receipt.getUpdatedAt()
        );
    }
}
