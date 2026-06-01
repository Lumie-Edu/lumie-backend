package com.lumie.tuition.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * PG-agnostic outbound port for tuition billing collection.
 *
 * <p>Models the PaySsam / "결제선생" integration shape: the academy sends an invoice
 * (guardian phone + amount + reason + due date); the external service is the merchant
 * of record and handles collection, then notifies status back. Lumie never handles
 * paymentKey / confirm / billing-key / sub-merchant KYC.
 *
 * <p>Implementations MUST be called OUTSIDE {@code @Transactional} boundaries
 * (HARD_RULES_CROSS: wrap external HTTP calls outside transactions).
 */
public interface TuitionBillingGatewayPort {

    /**
     * Sends a tuition invoice to the guardian via the billing gateway.
     *
     * @param orderId       our stable identifier (format: {@code TUITION-{invoiceId}-{suffix}})
     * @param guardianPhone guardian phone number (010-XXXX-XXXX)
     * @param guardianName  guardian name for display on the invoice
     * @param title         invoice title (e.g. "2026년 6월 수강료")
     * @param amount        total amount in KRW
     * @param dueDate       collection due date
     */
    SendInvoiceResult sendInvoice(String orderId, String guardianPhone, String guardianName,
                                  String title, BigDecimal amount, Instant dueDate);

    /**
     * Queries the current collection status for a previously sent invoice.
     *
     * @param externalInvoiceId the identifier returned by {@link #sendInvoice}
     */
    CollectionStatusResult queryStatus(String externalInvoiceId);

    record SendInvoiceResult(
            boolean success,
            String externalInvoiceId,
            String errorCode,
            String errorMessage
    ) {}

    record CollectionStatusResult(
            boolean found,
            String externalInvoiceId,
            String status,
            String errorCode,
            String errorMessage
    ) {}
}
