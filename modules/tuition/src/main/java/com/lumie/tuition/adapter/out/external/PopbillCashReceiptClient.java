package com.lumie.tuition.adapter.out.external;

import com.lumie.tuition.application.port.out.CashReceiptPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub implementation of CashReceiptPort (Popbill / Toss cash receipt API).
 *
 * TODO (Task 5 PoC): implement actual cash receipt issuance.
 * Options:
 * - Popbill SDK: https://developers.popbill.com/guide/cashbill/java
 * - Toss Payments cash receipt: POST /v1/cash-receipts
 *
 * NOTE: Must be called OUTSIDE @Transactional boundaries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopbillCashReceiptClient implements CashReceiptPort {

    @Override
    public CashReceiptIssueResult issue(CashReceiptIssueRequest req) {
        log.warn("STUB: PopbillCashReceiptClient.issue not implemented. orderId={}", req.orderId());
        // TODO: integrate Popbill or Toss cash receipt API
        return new CashReceiptIssueResult(
                false,
                null,
                null,
                "STUB_NOT_IMPLEMENTED",
                "STUB: Cash receipt issuance integration pending"
        );
    }
}
