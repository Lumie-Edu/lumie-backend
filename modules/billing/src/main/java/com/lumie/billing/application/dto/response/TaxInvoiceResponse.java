package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.entity.TaxInvoice;
import com.lumie.billing.domain.vo.TaxInvoiceIssueStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record TaxInvoiceResponse(
        Long id,
        @Schema(nullable = true) Long invoiceId,
        Long tenantId,
        String buyerBusinessNo,
        String supplierBusinessNo,
        long amount,
        long vatAmount,
        TaxInvoiceIssueStatus issueStatus,
        @Schema(nullable = true) String externalInvoiceId,
        @Schema(nullable = true) Instant issuedAt,
        Instant createdAt
) {
    public static TaxInvoiceResponse from(TaxInvoice taxInvoice) {
        return new TaxInvoiceResponse(
                taxInvoice.getId(),
                taxInvoice.getInvoiceId(),
                taxInvoice.getTenantId(),
                taxInvoice.getBuyerBusinessNo(),
                taxInvoice.getSupplierBusinessNo(),
                taxInvoice.getAmount().toLong(),
                taxInvoice.getVatAmount().toLong(),
                taxInvoice.getIssueStatus(),
                taxInvoice.getExternalInvoiceId(),
                taxInvoice.getIssuedAt(),
                taxInvoice.getCreatedAt()
        );
    }
}
