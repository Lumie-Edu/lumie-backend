package com.lumie.billing.application.service;

import com.lumie.billing.application.port.out.InvoicePersistencePort;
import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Transactional helper for invoice persistence.
 * Allows callers to commit invoice state before / after external PG calls.
 */
@Service
@RequiredArgsConstructor
public class InvoicePersistenceHelper {

    private final InvoicePersistencePort invoicePersistencePort;

    @Transactional
    public Invoice save(Invoice invoice) {
        return invoicePersistencePort.save(invoice);
    }

    @Transactional
    public void markPaid(Long invoiceId, String paymentKey, Instant paidAt) {
        Invoice invoice = invoicePersistencePort.findById(invoiceId)
                .orElseThrow(() -> new BillingException(BillingErrorCode.INVOICE_NOT_FOUND,
                        "Invoice not found: " + invoiceId));
        invoice.markPaid(paymentKey, paidAt);
        invoicePersistencePort.save(invoice);
    }

    @Transactional
    public void markFailed(Long invoiceId, String reason) {
        invoicePersistencePort.findById(invoiceId).ifPresent(inv -> {
            inv.markFailed(reason);
            invoicePersistencePort.save(inv);
        });
    }
}
