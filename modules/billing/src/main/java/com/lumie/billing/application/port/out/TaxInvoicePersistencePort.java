package com.lumie.billing.application.port.out;

import com.lumie.billing.domain.entity.TaxInvoice;
import com.lumie.billing.domain.vo.TaxInvoiceIssueStatus;

import java.util.List;
import java.util.Optional;

public interface TaxInvoicePersistencePort {
    TaxInvoice save(TaxInvoice taxInvoice);
    Optional<TaxInvoice> findById(Long id);
    Optional<TaxInvoice> findLatestByInvoiceId(Long invoiceId);
    List<TaxInvoice> findByTenantId(Long tenantId);
    List<TaxInvoice> findByIssueStatus(TaxInvoiceIssueStatus status);
}
