package com.lumie.billing.application.service;

import com.lumie.billing.application.port.out.TaxInvoicePersistencePort;
import com.lumie.billing.domain.entity.TaxInvoice;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Transactional helper for tax invoice persistence.
 */
@Service
@RequiredArgsConstructor
public class TaxInvoicePersistenceHelper {

    private final TaxInvoicePersistencePort taxInvoicePersistencePort;

    @Transactional
    public TaxInvoice save(TaxInvoice taxInvoice) {
        return taxInvoicePersistencePort.save(taxInvoice);
    }

    @Transactional
    public void markIssued(Long taxInvoiceId, String externalId, Instant issuedAt) {
        TaxInvoice taxInvoice = taxInvoicePersistencePort.findById(taxInvoiceId)
                .orElseThrow(() -> new BillingException(BillingErrorCode.TAX_INVOICE_NOT_FOUND,
                        "TaxInvoice not found: " + taxInvoiceId));
        taxInvoice.markIssued(externalId, issuedAt);
        taxInvoicePersistencePort.save(taxInvoice);
    }

    @Transactional
    public void markFailed(Long taxInvoiceId, String reason) {
        taxInvoicePersistencePort.findById(taxInvoiceId).ifPresent(ti -> {
            ti.markFailed(reason);
            taxInvoicePersistencePort.save(ti);
        });
    }
}
