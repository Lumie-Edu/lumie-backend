package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.vo.InvoiceStatus;
import lombok.Builder;

import java.time.Instant;
import com.lumie.common.util.Timestamps;

@Builder
public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        String tenantSlug,
        long amount,
        InvoiceStatus status,
        String description,
        Instant billingPeriodStart,
        Instant billingPeriodEnd,
        Instant dueDate,
        Instant paidAt,
        String paymentKey,
        Instant createdAt
) {
    public static InvoiceResponse from(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .tenantSlug(invoice.getTenantSlug())
                .amount(invoice.getAmount().toLong())
                .status(invoice.getStatus())
                .description(invoice.getDescription())
                .billingPeriodStart(Timestamps.utcInstant(invoice.getBillingPeriodStart()))
                .billingPeriodEnd(Timestamps.utcInstant(invoice.getBillingPeriodEnd()))
                .dueDate(Timestamps.utcInstant(invoice.getDueDate()))
                .paidAt(Timestamps.utcInstant(invoice.getPaidAt()))
                .paymentKey(invoice.getPaymentKey())
                .createdAt(Timestamps.utcInstant(invoice.getCreatedAt()))
                .build();
    }
}
