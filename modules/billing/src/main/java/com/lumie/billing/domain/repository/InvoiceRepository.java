package com.lumie.billing.domain.repository;

import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.vo.InvoiceStatus;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {
    Invoice save(Invoice invoice);
    Optional<Invoice> findById(Long id);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Optional<Invoice> findByOrderId(String orderId);
    List<Invoice> findByTenantSlug(String tenantSlug);
    List<Invoice> findBySubscriptionId(Long subscriptionId);
    List<Invoice> findByStatus(InvoiceStatus status);
    List<Invoice> findOverdueInvoices();
}
