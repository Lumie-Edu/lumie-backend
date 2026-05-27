package com.lumie.tuition.application.service;

import com.lumie.tuition.domain.vo.Money;
import com.lumie.tuition.application.dto.request.CancelInvoiceRequest;
import com.lumie.tuition.application.dto.request.IssueTuitionInvoiceRequest;
import com.lumie.tuition.application.dto.response.TuitionInvoiceResponse;
import com.lumie.tuition.domain.entity.TuitionInvoice;
import com.lumie.tuition.domain.exception.TuitionErrorCode;
import com.lumie.tuition.domain.exception.TuitionException;
import com.lumie.tuition.domain.repository.TuitionInvoiceRepository;
import com.lumie.tuition.domain.vo.LineItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TuitionInvoiceCommandService {

    private final TuitionInvoiceRepository invoiceRepository;

    public TuitionInvoiceResponse issueInvoice(IssueTuitionInvoiceRequest request) {
        String idempotencyKey = request.idempotencyKey();
        if (idempotencyKey != null && invoiceRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new TuitionException(TuitionErrorCode.IDEMPOTENCY_CONFLICT,
                    "Tuition invoice already exists for idempotency key: " + idempotencyKey);
        }

        List<LineItem> items = request.items();
        Money totalAmount = computeTotal(items);
        Money vatAmount = computeVat(totalAmount);

        TuitionInvoice invoice = TuitionInvoice.create(
                request.studentId(),
                request.guardianId(),
                request.classEnrollmentId(),
                request.title(),
                items,
                totalAmount,
                vatAmount,
                idempotencyKey
        );
        invoice.issue(request.dueDate());

        TuitionInvoice saved = invoiceRepository.save(invoice);
        log.info("TuitionInvoice issued: id={}, studentId={}, amount={}",
                saved.getId(), saved.getStudentId(), totalAmount);
        return TuitionInvoiceResponse.from(saved);
    }

    public TuitionInvoiceResponse markPaid(Long invoiceId, Instant paidAt) {
        TuitionInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.INVOICE_NOT_FOUND,
                        "TuitionInvoice not found: " + invoiceId));
        invoice.markPaid(paidAt);
        TuitionInvoice saved = invoiceRepository.save(invoice);
        log.info("TuitionInvoice marked paid: id={}", invoiceId);
        return TuitionInvoiceResponse.from(saved);
    }

    public TuitionInvoiceResponse cancelInvoice(Long invoiceId, CancelInvoiceRequest request) {
        TuitionInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new TuitionException(TuitionErrorCode.INVOICE_NOT_FOUND,
                        "TuitionInvoice not found: " + invoiceId));
        invoice.cancel(request.reason());
        TuitionInvoice saved = invoiceRepository.save(invoice);
        log.info("TuitionInvoice cancelled: id={}, reason={}", invoiceId, request.reason());
        return TuitionInvoiceResponse.from(saved);
    }

    /**
     * Marks all ISSUED invoices past their dueDate as OVERDUE within the current tenant context.
     * Called by OverdueNotificationScheduler — must be invoked on the Spring proxy (different bean)
     * so @Transactional applies correctly.
     *
     * @return count of invoices newly marked OVERDUE
     */
    public int markOverdueInvoices() {
        Instant now = Instant.now();
        List<TuitionInvoice> overdueEligible = invoiceRepository.findOverdueEligible(now);
        int count = 0;
        for (TuitionInvoice invoice : overdueEligible) {
            try {
                invoice.markOverdue();
                invoiceRepository.save(invoice);
                count++;
            } catch (Exception e) {
                log.error("Failed to mark invoice {} overdue: {}", invoice.getId(), e.getMessage());
            }
        }
        return count;
    }

    private Money computeTotal(List<LineItem> items) {
        BigDecimal total = items.stream()
                .map(item -> item.amount().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Money.of(total);
    }

    private Money computeVat(Money totalAmount) {
        // Korean VAT = 10%
        BigDecimal vat = totalAmount.getAmount()
                .multiply(BigDecimal.valueOf(10))
                .divide(BigDecimal.valueOf(110), 0, java.math.RoundingMode.DOWN);
        return Money.of(vat);
    }
}
