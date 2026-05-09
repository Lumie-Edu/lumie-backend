package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.vo.InvoiceStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        String tenantSlug,
        long amount,
        InvoiceStatus status,
        String description,
        LocalDateTime billingPeriodStart,
        LocalDateTime billingPeriodEnd,
        LocalDateTime dueDate,
        LocalDateTime paidAt,
        String paymentKey,
        LocalDateTime createdAt
) {
    public static InvoiceResponse from(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .tenantSlug(invoice.getTenantSlug())
                .amount(invoice.getAmount().toLong())
                .status(invoice.getStatus())
                .description(invoice.getDescription())
                .billingPeriodStart(invoice.getBillingPeriodStart())
                .billingPeriodEnd(invoice.getBillingPeriodEnd())
                .dueDate(invoice.getDueDate())
                .paidAt(invoice.getPaidAt())
                .paymentKey(invoice.getPaymentKey())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
