package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.response.TaxInvoiceResponse;
import com.lumie.billing.application.port.out.InvoicePersistencePort;
import com.lumie.billing.application.port.out.TaxInvoicePersistencePort;
import com.lumie.billing.application.port.out.TaxInvoicePort;
import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.entity.TaxInvoice;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.TaxInvoiceIssueStatus;
import com.lumie.billing.domain.vo.TransactionDirection;
import com.lumie.billing.domain.vo.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

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
    private final PaymentTransactionLogger transactionLogger;
    private final BillingOperationLockService operationLockService;

    /**
     * Issues a tax invoice for a billing Invoice that has been paid.
     * Existing attempts are retained; a failed attempt may be retried with the same provider key.
     */
    public TaxInvoiceResponse issueForInvoice(Long invoiceId, String buyerBusinessNo) {
        Invoice invoice = invoicePersistencePort.findById(invoiceId)
                .orElseThrow(() -> new BillingException(BillingErrorCode.INVOICE_NOT_FOUND,
                        "Invoice not found: " + invoiceId));

        if (!invoice.isPaid()) {
            throw new BillingException(BillingErrorCode.INVALID_INVOICE_STATE,
                    "Tax invoice can only be issued for a PAID invoice");
        }

        String requestId = "tax-invoice-" + invoiceId;
        return operationLockService.executeExclusively(invoice.getTenantId(),
                "TAX_INVOICE_ISSUE_" + invoiceId, requestId,
                () -> issueExclusively(invoice, buyerBusinessNo, requestId));
    }

    private TaxInvoiceResponse issueExclusively(Invoice invoice, String buyerBusinessNo, String requestId) {
        Long invoiceId = invoice.getId();
        taxInvoicePersistencePort.findLatestByInvoiceId(invoiceId).ifPresent(existing -> {
            if (existing.isIssued()) {
                throw new BillingException(BillingErrorCode.TAX_INVOICE_ALREADY_ISSUED,
                        "Tax invoice already issued for invoice: " + invoiceId);
            }
            if (existing.getIssueStatus() != TaxInvoiceIssueStatus.FAILED) {
                throw new BillingException(BillingErrorCode.OPERATION_IN_PROGRESS,
                        "Tax invoice issue attempt is still pending for invoice: " + invoiceId);
            }
        });

        TaxInvoice taxInvoice = TaxInvoice.create(
                invoiceId, invoice.getTenantId(),
                buyerBusinessNo, supplierBusinessNo,
                invoice.getAmount(), invoice.getVatAmount()
        );

        TaxInvoice saved = taxInvoicePersistenceHelper.save(taxInvoice);

        // Provider call outside @Transactional; invoice-scoped key deduplicates retries.
        TaxInvoicePort.TaxInvoiceRequest request = new TaxInvoicePort.TaxInvoiceRequest(
                buyerBusinessNo,
                "테넌트 " + invoice.getTenantId(),
                supplierBusinessNo,
                "Lumie",
                BigDecimal.valueOf(invoice.getAmount().toLong()),
                BigDecimal.valueOf(invoice.getVatAmount().toLong()),
                invoice.getInvoiceNumber(),
                requestId
        );

        transactionLogger.log(invoice.getTenantId(), TransactionType.TAX_INVOICE_ISSUE,
                TransactionDirection.REQUEST, requestId, "TaxInvoice", saved.getId(),
                Map.of("invoiceId", invoiceId, "taxInvoiceId", saved.getId(),
                        "amount", invoice.getAmount().toLong(), "vatAmount", invoice.getVatAmount().toLong()),
                null);

        TaxInvoicePort.TaxInvoiceResult result;
        try {
            result = taxInvoicePort.issue(request);
        } catch (RuntimeException exception) {
            String failureReason = exception.getClass().getSimpleName();
            transactionLogger.log(invoice.getTenantId(), TransactionType.TAX_INVOICE_ISSUE,
                    TransactionDirection.RESPONSE, requestId, "TaxInvoice", saved.getId(),
                    Map.of("success", false, "error", failureReason), null);
            taxInvoicePersistenceHelper.markFailed(saved.getId(), failureReason);
            throw new BillingException(BillingErrorCode.TAX_INVOICE_ISSUE_FAILED,
                    "Tax invoice provider call failed: " + failureReason);
        }

        transactionLogger.log(invoice.getTenantId(), TransactionType.TAX_INVOICE_ISSUE,
                TransactionDirection.RESPONSE, requestId, "TaxInvoice", saved.getId(),
                Map.of("success", result.success(),
                        "externalInvoiceId", result.externalInvoiceId() != null ? result.externalInvoiceId() : "",
                        "error", result.errorMessage() != null ? result.errorMessage() : ""),
                null);

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
