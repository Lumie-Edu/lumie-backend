package com.lumie.tuition.application.service;

import com.lumie.tuition.application.dto.request.RequestPaymentRequest;
import com.lumie.tuition.domain.entity.TuitionInvoice;
import com.lumie.tuition.domain.entity.TuitionPayment;
import com.lumie.tuition.domain.vo.Money;
import com.lumie.tuition.domain.repository.TuitionPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional helper for TuitionPaymentCommandService.
 * Extracted to a separate bean so Spring proxy can apply @Transactional correctly
 * (self-invocation from TuitionPaymentCommandService would bypass the proxy).
 */
@Service
@RequiredArgsConstructor
public class TuitionPaymentPersistenceHelper {

    private final TuitionPaymentRepository paymentRepository;

    @Transactional
    public TuitionPayment savePendingPayment(TuitionInvoice invoice, String orderId,
                                              RequestPaymentRequest request) {
        TuitionPayment payment = TuitionPayment.create(
                invoice.getId(),
                orderId,
                invoice.getTotalAmount(),
                request.method()
        );
        return paymentRepository.save(payment);
    }

    @Transactional
    public void failPendingPayment(Long paymentId, String reason) {
        paymentRepository.findById(paymentId).ifPresent(p -> {
            p.fail(reason);
            paymentRepository.save(p);
        });
    }

    @Transactional
    public TuitionPayment applyRefundToPayment(Long paymentId, Money refundAmount,
                                                String reason, Long staffId,
                                                com.lumie.tuition.domain.exception.TuitionErrorCode errorCode) {
        TuitionPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new com.lumie.tuition.domain.exception.TuitionException(errorCode,
                        "TuitionPayment not found: " + paymentId));
        payment.refund(refundAmount, reason, staffId);
        return paymentRepository.save(payment);
    }
}
