package com.lumie.tuition.application.port.out;

import com.lumie.tuition.domain.vo.CashReceiptType;

import java.math.BigDecimal;

/**
 * Outbound port for cash receipt issuance (Popbill, Toss, etc.).
 * Implementations MUST be called OUTSIDE @Transactional boundaries.
 */
public interface CashReceiptPort {

    CashReceiptIssueResult issue(CashReceiptIssueRequest req);

    record CashReceiptIssueRequest(
            String orderId,
            CashReceiptType receiptType,
            String identifier,
            BigDecimal amount,
            BigDecimal vatAmount,
            String customerName
    ) {}

    record CashReceiptIssueResult(
            boolean success,
            String approvalNumber,
            String externalId,
            String errorCode,
            String errorMessage
    ) {}
}
