package com.lumie.billing.application.port.out;

import java.math.BigDecimal;

/**
 * Outbound port for Korean tax invoice (세금계산서) issuance via Popbill or similar SaaS.
 * Implementations must be called OUTSIDE @Transactional boundaries.
 */
public interface TaxInvoicePort {

    TaxInvoiceResult issue(TaxInvoiceRequest request);

    record TaxInvoiceRequest(
            String buyerBusinessNo,
            String buyerName,
            String supplierBusinessNo,
            String supplierName,
            BigDecimal amount,
            BigDecimal vatAmount,
            String description,
            String idempotencyKey
    ) {}

    record TaxInvoiceResult(
            boolean success,
            String externalInvoiceId,
            String errorMessage
    ) {}
}
