package com.lumie.billing.application.port.out;

import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.vo.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface InvoicePersistencePort {
    Invoice save(Invoice invoice);
    Optional<Invoice> findById(Long id);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Optional<Invoice> findByOrderId(String orderId);
    /** Paged — use for controller endpoints. */
    Page<Invoice> findByTenantId(Long tenantId, Pageable pageable);
    /** Paged — use for controller endpoints. */
    Page<Invoice> findByTenantSlug(String tenantSlug, Pageable pageable);
    /** Unbounded — internal use (scheduler, etc.). */
    List<Invoice> findByTenantId(Long tenantId);
    /** Unbounded — internal use. */
    List<Invoice> findByTenantSlug(String tenantSlug);
    List<Invoice> findBySubscriptionId(Long subscriptionId);
    List<Invoice> findByStatus(InvoiceStatus status);
    List<Invoice> findOverdueInvoices();
}
