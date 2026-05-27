package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.application.port.out.TaxInvoicePersistencePort;
import com.lumie.billing.domain.entity.TaxInvoice;
import com.lumie.billing.domain.vo.TaxInvoiceIssueStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TaxInvoicePersistenceAdapter implements TaxInvoicePersistencePort {

    private final TaxInvoiceJpaRepository taxInvoiceJpaRepository;

    @Override
    public TaxInvoice save(TaxInvoice taxInvoice) {
        return taxInvoiceJpaRepository.save(taxInvoice);
    }

    @Override
    public Optional<TaxInvoice> findById(Long id) {
        return taxInvoiceJpaRepository.findById(id);
    }

    @Override
    public Optional<TaxInvoice> findByInvoiceId(Long invoiceId) {
        return taxInvoiceJpaRepository.findByInvoiceId(invoiceId);
    }

    @Override
    public List<TaxInvoice> findByTenantId(Long tenantId) {
        return taxInvoiceJpaRepository.findByTenantId(tenantId);
    }

    @Override
    public List<TaxInvoice> findByIssueStatus(TaxInvoiceIssueStatus status) {
        return taxInvoiceJpaRepository.findByIssueStatus(status);
    }
}
