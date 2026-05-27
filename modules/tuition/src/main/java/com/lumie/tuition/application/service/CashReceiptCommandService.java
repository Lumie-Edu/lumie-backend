package com.lumie.tuition.application.service;

import com.lumie.tuition.domain.vo.Money;
import com.lumie.tuition.application.dto.request.IssueCashReceiptRequest;
import com.lumie.tuition.application.dto.response.CashReceiptResponse;
import com.lumie.tuition.application.port.out.CashReceiptPort;
import com.lumie.tuition.domain.entity.CashReceipt;
import com.lumie.tuition.domain.entity.TuitionPayment;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.repository.CashReceiptRepository;
import com.lumie.tuition.domain.repository.TuitionPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashReceiptCommandService {

    private final CashReceiptRepository cashReceiptRepository;
    private final TuitionPaymentRepository paymentRepository;
    private final CashReceiptPort cashReceiptPort;
    private final CashReceiptPersistenceHelper persistenceHelper;

    /**
     * Issues a cash receipt for a captured payment.
     * DB record is persisted first (PENDING) via helper bean, then the external API
     * is called outside @Transactional (HARD_RULES: no HTTP inside transaction).
     */
    public CashReceiptResponse issueForPayment(IssueCashReceiptRequest request) {
        TuitionPayment payment = paymentRepository.findById(request.tuitionPaymentId())
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.PAYMENT_NOT_FOUND,
                        "TuitionPayment not found: " + request.tuitionPaymentId()));

        if (!payment.isCaptured()) {
            throw new TuitionException(TuitionErrorCode.INVALID_PAYMENT_STATUS,
                    "Cash receipt can only be issued for CAPTURED payments");
        }

        // Step 1: persist PENDING record (via helper bean — separate bean = no self-invocation)
        CashReceipt receipt = persistenceHelper.savePendingReceipt(payment, request);

        // Step 2: call external provider outside @Transactional
        CashReceiptPort.CashReceiptIssueRequest issueRequest = new CashReceiptPort.CashReceiptIssueRequest(
                payment.getOrderId(),
                request.receiptType(),
                request.identifier(),
                payment.getAmount().getAmount(),
                Money.zero().getAmount(),
                request.customerName()
        );
        CashReceiptPort.CashReceiptIssueResult result = cashReceiptPort.issue(issueRequest);

        // Step 3: update record based on result (via helper bean — separate transaction)
        CashReceipt updated = persistenceHelper.updateReceiptResult(receipt.getId(), result);

        log.info("CashReceipt issued: id={}, paymentId={}, status={}",
                updated.getId(), payment.getId(), updated.getIssueStatus());
        return CashReceiptResponse.from(updated);
    }
}
