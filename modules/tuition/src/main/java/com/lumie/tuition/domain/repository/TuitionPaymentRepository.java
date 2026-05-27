package com.lumie.tuition.domain.repository;

import com.lumie.tuition.domain.entity.TuitionPayment;
import com.lumie.tuition.domain.vo.TuitionPaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TuitionPaymentRepository {

    TuitionPayment save(TuitionPayment payment);

    Optional<TuitionPayment> findById(Long id);

    Optional<TuitionPayment> findByOrderId(String orderId);

    Optional<TuitionPayment> findByPgTransactionId(String pgTransactionId);

    List<TuitionPayment> findByTuitionInvoiceId(Long tuitionInvoiceId);

    Page<TuitionPayment> findByStatus(TuitionPaymentStatus status, Pageable pageable);

    Page<TuitionPayment> findAll(Pageable pageable);

    boolean existsByOrderId(String orderId);
}
