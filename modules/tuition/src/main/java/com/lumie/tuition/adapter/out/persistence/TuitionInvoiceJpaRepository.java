package com.lumie.tuition.adapter.out.persistence;

import com.lumie.tuition.domain.entity.TuitionInvoice;
import com.lumie.tuition.domain.repository.TuitionInvoiceRepository;
import com.lumie.tuition.domain.vo.TuitionInvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TuitionInvoiceJpaRepository
        extends TuitionInvoiceRepository, JpaRepository<TuitionInvoice, Long> {

    @Override
    @Query("SELECT i FROM TuitionInvoice i WHERE i.status = com.lumie.tuition.domain.vo.TuitionInvoiceStatus.ISSUED AND i.dueDate < :now")
    List<TuitionInvoice> findOverdueEligible(@Param("now") Instant now);

    @Override
    @Query("SELECT i FROM TuitionInvoice i WHERE i.status IN (com.lumie.tuition.domain.vo.TuitionInvoiceStatus.ISSUED, com.lumie.tuition.domain.vo.TuitionInvoiceStatus.OVERDUE) AND i.dueDate < :cutoff")
    List<TuitionInvoice> findUnpaidPastDue(@Param("cutoff") Instant cutoff);
}
