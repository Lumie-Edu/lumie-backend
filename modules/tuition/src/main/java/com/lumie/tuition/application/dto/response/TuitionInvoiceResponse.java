package com.lumie.tuition.application.dto.response;

import com.lumie.tuition.domain.entity.TuitionInvoice;
import com.lumie.tuition.domain.vo.LineItem;
import com.lumie.tuition.domain.vo.TuitionInvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TuitionInvoiceResponse(
        Long id,
        Long studentId,
        Long guardianId,
        Long classEnrollmentId,
        String title,
        List<LineItem> items,
        BigDecimal totalAmount,
        BigDecimal vatAmount,
        TuitionInvoiceStatus status,
        Instant issuedAt,
        Instant dueDate,
        Instant paidAt,
        Instant cancelledAt,
        String cancellationReason,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt
) {
    public static TuitionInvoiceResponse from(TuitionInvoice invoice) {
        return new TuitionInvoiceResponse(
                invoice.getId(),
                invoice.getStudentId(),
                invoice.getGuardianId(),
                invoice.getClassEnrollmentId(),
                invoice.getTitle(),
                invoice.getItems(),
                invoice.getTotalAmount() != null ? invoice.getTotalAmount().getAmount() : null,
                invoice.getVatAmount() != null ? invoice.getVatAmount().getAmount() : null,
                invoice.getStatus(),
                invoice.getIssuedAt(),
                invoice.getDueDate(),
                invoice.getPaidAt(),
                invoice.getCancelledAt(),
                invoice.getCancellationReason(),
                invoice.getIdempotencyKey(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }
}
