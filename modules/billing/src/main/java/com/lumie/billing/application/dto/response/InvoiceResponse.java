package com.lumie.billing.application.dto.response;

import com.lumie.billing.domain.entity.Invoice;
import com.lumie.billing.domain.vo.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        Long subscriptionId,
        Long tenantId,
        long amount,
        long vatAmount,
        InvoiceStatus status,
        boolean overdue,
        @Schema(nullable = true) String description,
        @Schema(nullable = true) Instant billingPeriodStart,
        @Schema(nullable = true) Instant billingPeriodEnd,
        @Schema(nullable = true) Instant dueDate,
        @Schema(nullable = true) Instant paidAt,
        @Schema(nullable = true) String paymentKey,
        Instant createdAt
) {
    public static InvoiceResponse from(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .subscriptionId(invoice.getSubscriptionId())
                .tenantId(invoice.getTenantId())
                .amount(invoice.getAmount().toLong())
                .vatAmount(invoice.getVatAmount().toLong())
                .status(invoice.getStatus())
                .overdue(invoice.isOverdue())
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
