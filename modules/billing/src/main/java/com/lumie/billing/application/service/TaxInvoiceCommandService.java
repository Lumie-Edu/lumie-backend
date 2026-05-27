package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.response.TaxInvoiceResponse;
import com.lumie.billing.application.port.out.InvoicePersistencePort;
import com.lumie.billing.application.port.out.TaxInvoicePersistencePort;
import com.lumie.billing.application.port.out.TaxInvoicePort;
import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.entity.TaxInvoice;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Issues Korean tax invoices (세금계산서) via Popbill.
 * No class-level @Transactional — TaxInvoicePort calls must stay outside transactions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxInvoiceCommandService {

    @Value("${billing.tax-invoice.supplier-business-no:000-00-00000}")
    private String supplierBusinessNo;

    private final InvoicePersistencePort invoicePersistencePort;
    private final TaxInvoicePersistencePort taxInvoicePersistencePort;
    private final TaxInvoicePersistenceHelper taxInvoicePersistenceHelper;
    private final TaxInvoicePort taxInvoicePort;

    /**
     * Issues a tax invoice for a billing Invoice that has been paid.
     * Called from the Toss billing webhook handler after a successful payment.
     */
    public TaxInvoiceResponse issueForInvoice(Long invoiceId, String buyerBusinessNo) {
        Invoice invoice = invoicePersistencePort.findById(invoiceId)
                .orElseThrow(() -> new BillingException(BillingErrorCode.INVOICE_NOT_FOUND,
                        "Invoice not found: " + invoiceId));

        if (!invoice.isPaid()) {
            throw new BillingException(BillingErrorCode.INVALID_INVOICE_STATE,
                    "Tax invoice can only be issued for a PAID invoice");
        }

        // Check for existing tax invoice
        taxInvoicePersistencePort.findByInvoiceId(invoiceId).ifPresent(existing -> {
            if (existing.isIssued()) {
                throw new BillingException(BillingErrorCode.TAX_INVOICE_ALREADY_ISSUED,
                        "Tax invoice already issued for invoice: " + invoiceId);
            }
        });

        TaxInvoice taxInvoice = TaxInvoice.create(
                invoiceId, invoice.getTenantId(),
                buyerBusinessNo, supplierBusinessNo,
                invoice.getAmount(), invoice.getVatAmount()
        );

        TaxInvoice saved = taxInvoicePersistenceHelper.save(taxInvoice);

        // PG call outside @Transactional
        TaxInvoicePort.TaxInvoiceRequest request = new TaxInvoicePort.TaxInvoiceRequest(
                buyerBusinessNo,
                "테넌트 " + invoice.getTenantId(),
                supplierBusinessNo,
                "Lumie",
                BigDecimal.valueOf(invoice.getAmount().toLong()),
                BigDecimal.valueOf(invoice.getVatAmount().toLong()),
                invoice.getInvoiceNumber(),
                UUID.randomUUID().toString()
        );

        TaxInvoicePort.TaxInvoiceResult result = taxInvoicePort.issue(request);

        if (result.success()) {
            taxInvoicePersistenceHelper.markIssued(saved.getId(), result.externalInvoiceId(), Instant.now());
            log.info("TaxInvoice issued: taxInvoiceId={}, invoiceId={}, externalId={}",
                    saved.getId(), invoiceId, result.externalInvoiceId());
        } else {
            taxInvoicePersistenceHelper.markFailed(saved.getId(), result.errorMessage());
            log.error("TaxInvoice issuance failed: taxInvoiceId={}, invoiceId={}, error={}",
                    saved.getId(), invoiceId, result.errorMessage());
            throw new BillingException(BillingErrorCode.TAX_INVOICE_ISSUE_FAILED,
                    "Tax invoice issuance failed: " + result.errorMessage());
        }

        return taxInvoicePersistencePort.findById(saved.getId())
                .map(TaxInvoiceResponse::from)
                .orElseThrow();
    }
}
