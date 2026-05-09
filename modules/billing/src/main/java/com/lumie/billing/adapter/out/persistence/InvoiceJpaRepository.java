package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.vo.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceJpaRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByOrderId(String orderId);

    @Query("SELECT i FROM Invoice i WHERE i.tenantSlug = :tenantSlug ORDER BY i.createdAt DESC")
    List<Invoice> findByTenantSlug(@Param("tenantSlug") String tenantSlug);

    @Query("SELECT i FROM Invoice i WHERE i.subscription.id = :subscriptionId ORDER BY i.createdAt DESC")
    List<Invoice> findBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    List<Invoice> findByStatus(InvoiceStatus status);

    @Query("SELECT i FROM Invoice i WHERE i.status = 'PENDING' AND i.dueDate < CURRENT_TIMESTAMP")
    List<Invoice> findOverdueInvoices();
}
