package com.lumie.tuition.application.service;

import com.lumie.tuition.application.dto.response.TuitionPaymentResponse;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.repository.TuitionPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TuitionPaymentQueryService {

    private final TuitionPaymentRepository paymentRepository;

    public TuitionPaymentResponse getById(Long id) {
        return paymentRepository.findById(id)
                .map(TuitionPaymentResponse::from)
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.PAYMENT_NOT_FOUND,
                        "TuitionPayment not found: " + id));
    }

    public List<TuitionPaymentResponse> listByInvoice(Long invoiceId) {
        return paymentRepository.findByTuitionInvoiceId(invoiceId)
                .stream()
                .map(TuitionPaymentResponse::from)
                .toList();
    }

    public Page<TuitionPaymentResponse> listAll(Pageable pageable) {
        return paymentRepository.findAll(pageable)
                .map(TuitionPaymentResponse::from);
    }
}
