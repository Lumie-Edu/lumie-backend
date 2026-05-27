package com.lumie.tuition.domain.repository;

import com.lumie.tuition.domain.entity.CashReceipt;
import com.lumie.tuition.domain.vo.CashReceiptIssueStatus;

import java.util.List;
import java.util.Optional;

public interface CashReceiptRepository {

    CashReceipt save(CashReceipt receipt);

    Optional<CashReceipt> findById(Long id);

    Optional<CashReceipt> findByTuitionPaymentId(Long tuitionPaymentId);

    List<CashReceipt> findByIssueStatus(CashReceiptIssueStatus issueStatus);
}
