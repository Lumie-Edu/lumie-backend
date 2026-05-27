package com.lumie.tuition.adapter.out.persistence;

import com.lumie.tuition.domain.entity.TuitionPayment;
import com.lumie.tuition.domain.repository.TuitionPaymentRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TuitionPaymentJpaRepository
        extends TuitionPaymentRepository, JpaRepository<TuitionPayment, Long> {
}
