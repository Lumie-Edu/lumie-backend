package com.lumie.tuition.application.service;

import com.lumie.tuition.application.dto.request.IssueCashReceiptRequest;
import com.lumie.tuition.application.port.out.CashReceiptPort;
import com.lumie.tuition.domain.entity.CashReceipt;
import com.lumie.tuition.domain.entity.TuitionPayment;
import com.lumie.tuition.domain.repository.CashReceiptRepository;
import com.lumie.tuition.domain.vo.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional helper for CashReceiptCommandService.
 * Extracted to a separate bean so Spring proxy can apply @Transactional correctly
 * (self-invocation from CashReceiptCommandService would bypass the proxy).
 */
@Service
@RequiredArgsConstructor
public class CashReceiptPersistenceHelper {

    private final CashReceiptRepository cashReceiptRepository;

    @Transactional
    public CashReceipt savePendingReceipt(TuitionPayment payment, IssueCashReceiptRequest request) {
        CashReceipt receipt = CashReceipt.create(
                payment.getId(),
                request.receiptType(),
                request.identifier(),
                payment.getAmount(),
                Money.zero()
        );
        return cashReceiptRepository.save(receipt);
    }

    @Transactional
    public CashReceipt updateReceiptResult(Long receiptId, CashReceiptPort.CashReceiptIssueResult result) {
        CashReceipt receipt = cashReceiptRepository.findById(receiptId).orElseThrow();
        if (result.success()) {
            receipt.markIssued(result.approvalNumber(), result.externalId());
        } else {
            receipt.markFailed(result.errorMessage());
        }
        return cashReceiptRepository.save(receipt);
    }
}
