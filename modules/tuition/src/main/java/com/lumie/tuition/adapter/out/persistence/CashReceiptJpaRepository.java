package com.lumie.tuition.adapter.out.persistence;

import com.lumie.tuition.domain.entity.CashReceipt;
import com.lumie.tuition.domain.repository.CashReceiptRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CashReceiptJpaRepository
        extends CashReceiptRepository, JpaRepository<CashReceipt, Long> {
}
