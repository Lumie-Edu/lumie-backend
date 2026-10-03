package com.lumie.billing.adapter.out.persistence;

import com.lumie.billing.domain.entity.TaxInvoice;
import com.lumie.billing.domain.vo.TaxInvoiceIssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxInvoiceJpaRepository extends JpaRepository<TaxInvoice, Long> {

    Optional<TaxInvoice> findTopByInvoiceIdOrderByIdDesc(Long invoiceId);

    List<TaxInvoice> findByTenantId(Long tenantId);

    List<TaxInvoice> findByIssueStatus(TaxInvoiceIssueStatus issueStatus);
}
