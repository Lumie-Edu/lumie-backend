package com.lumie.billing.adapter.out.external;

import com.lumie.billing.application.port.out.TaxInvoicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub implementation of TaxInvoicePort (Popbill 세금계산서 SaaS).
 *
 * TODO (Task 5 PoC): implement actual Popbill integration.
 * When implementing:
 * 1. Add to Vault: secret/applications/popbill (linkId, secretKey, corpNum)
 * 2. Use Popbill Java SDK: https://github.com/linkhub-sdk/popbill.sdk.java
 * 3. API: TaxinvoiceService.registIssue(...)
 *
 * NOTE: Must be called OUTSIDE @Transactional boundaries (HARD_RULES).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopbillTaxInvoiceClient implements TaxInvoicePort {

    @Override
    public TaxInvoiceResult issue(TaxInvoiceRequest request) {
        log.warn("STUB: PopbillTaxInvoiceClient.issue not implemented. buyerBusinessNo={}",
                request.buyerBusinessNo());
        // TODO: call Popbill SDK TaxinvoiceService.registIssue
        return new TaxInvoiceResult(
                true,
                "STUB-EXT-INV-" + request.idempotencyKey().substring(0, 8),
                "STUB: Popbill tax invoice integration pending"
        );
    }
}
