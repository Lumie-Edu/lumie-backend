package com.lumie.tuition.domain.repository;

import com.lumie.tuition.domain.entity.TuitionInvoice;
import com.lumie.tuition.domain.vo.TuitionInvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TuitionInvoiceRepository {

    TuitionInvoice save(TuitionInvoice invoice);

    Optional<TuitionInvoice> findById(Long id);

    Optional<TuitionInvoice> findByIdempotencyKey(String idempotencyKey);

    Page<TuitionInvoice> findByStudentId(Long studentId, Pageable pageable);

    Page<TuitionInvoice> findByGuardianId(Long guardianId, Pageable pageable);

    Page<TuitionInvoice> findByStatus(TuitionInvoiceStatus status, Pageable pageable);

    List<TuitionInvoice> findByStatus(TuitionInvoiceStatus status);

    Page<TuitionInvoice> findAll(Pageable pageable);

    /**
     * Returns invoices that are ISSUED and whose due_date is before the given cutoff,
     * eligible to be marked OVERDUE.
     */
    List<TuitionInvoice> findOverdueEligible(Instant now);

    /**
     * Returns invoices that are ISSUED or OVERDUE — used by the overdue notification scheduler.
     */
    List<TuitionInvoice> findUnpaidPastDue(Instant cutoff);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
