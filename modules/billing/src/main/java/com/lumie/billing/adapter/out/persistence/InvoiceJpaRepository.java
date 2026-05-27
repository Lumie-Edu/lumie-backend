package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.vo.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceJpaRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByOrderId(String orderId);

    // ─── Paged variants (for controller endpoints) ─────────────────────────────

    @Query("SELECT i FROM Invoice i WHERE i.tenantId = :tenantId")
    Page<Invoice> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.tenantId = " +
           "(SELECT s.tenantId FROM Subscription s WHERE s.tenantSlug = :tenantSlug)")
    Page<Invoice> findByTenantSlug(@Param("tenantSlug") String tenantSlug, Pageable pageable);

    // ─── Unbounded variants (scheduler / internal) ─────────────────────────────

    @Query("SELECT i FROM Invoice i WHERE i.tenantId = :tenantId ORDER BY i.createdAt DESC")
    List<Invoice> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT i FROM Invoice i WHERE i.tenantId = " +
           "(SELECT s.tenantId FROM Subscription s WHERE s.tenantSlug = :tenantSlug) " +
           "ORDER BY i.createdAt DESC")
    List<Invoice> findAllByTenantSlug(@Param("tenantSlug") String tenantSlug);

    @Query("SELECT i FROM Invoice i WHERE i.subscriptionId = :subscriptionId ORDER BY i.createdAt DESC")
    List<Invoice> findBySubscriptionId(@Param("subscriptionId") Long subscriptionId);

    List<Invoice> findByStatus(InvoiceStatus status);

    @Query("SELECT i FROM Invoice i " +
           "WHERE i.status = com.lumie.billing.domain.vo.InvoiceStatus.PENDING " +
           "AND i.dueDate < CURRENT_TIMESTAMP")
    List<Invoice> findOverdueInvoices();
}
