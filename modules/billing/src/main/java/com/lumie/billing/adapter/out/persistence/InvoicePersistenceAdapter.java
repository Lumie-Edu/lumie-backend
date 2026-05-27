package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.application.port.out.InvoicePersistencePort;
import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.vo.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InvoicePersistenceAdapter implements InvoicePersistencePort {

    private final InvoiceJpaRepository invoiceJpaRepository;

    @Override
    public Invoice save(Invoice invoice) {
        return invoiceJpaRepository.save(invoice);
    }

    @Override
    public Optional<Invoice> findById(Long id) {
        return invoiceJpaRepository.findById(id);
    }

    @Override
    public Optional<Invoice> findByInvoiceNumber(String invoiceNumber) {
        return invoiceJpaRepository.findByInvoiceNumber(invoiceNumber);
    }

    @Override
    public Optional<Invoice> findByOrderId(String orderId) {
        return invoiceJpaRepository.findByOrderId(orderId);
    }

    @Override
    public Page<Invoice> findByTenantId(Long tenantId, Pageable pageable) {
        return invoiceJpaRepository.findByTenantId(tenantId, pageable);
    }

    @Override
    public Page<Invoice> findByTenantSlug(String tenantSlug, Pageable pageable) {
        return invoiceJpaRepository.findByTenantSlug(tenantSlug, pageable);
    }

    @Override
    public List<Invoice> findByTenantId(Long tenantId) {
        return invoiceJpaRepository.findAllByTenantId(tenantId);
    }

    @Override
    public List<Invoice> findByTenantSlug(String tenantSlug) {
        return invoiceJpaRepository.findAllByTenantSlug(tenantSlug);
    }

    @Override
    public List<Invoice> findBySubscriptionId(Long subscriptionId) {
        return invoiceJpaRepository.findBySubscriptionId(subscriptionId);
    }

    @Override
    public List<Invoice> findByStatus(InvoiceStatus status) {
        return invoiceJpaRepository.findByStatus(status);
    }

    @Override
    public List<Invoice> findOverdueInvoices() {
        return invoiceJpaRepository.findOverdueInvoices();
    }
}
