package com.lumie.tuition.application.service;

import com.lumie.tuition.application.dto.response.TuitionInvoiceResponse;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.repository.TuitionInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TuitionInvoiceQueryService {

    private final TuitionInvoiceRepository invoiceRepository;

    public TuitionInvoiceResponse getById(Long id) {
        return invoiceRepository.findById(id)
                .map(TuitionInvoiceResponse::from)
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.INVOICE_NOT_FOUND,
                        "TuitionInvoice not found: " + id));
    }

    public Page<TuitionInvoiceResponse> listByStudent(Long studentId, Pageable pageable) {
        return invoiceRepository.findByStudentId(studentId, pageable)
                .map(TuitionInvoiceResponse::from);
    }

    public Page<TuitionInvoiceResponse> listAll(Pageable pageable) {
        return invoiceRepository.findAll(pageable)
                .map(TuitionInvoiceResponse::from);
    }
}
