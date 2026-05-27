package com.lumie.tuition.application.dto.request;

import com.lumie.tuition.domain.vo.LineItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record IssueTuitionInvoiceRequest(
        @NotNull Long studentId,
        Long guardianId,
        Long classEnrollmentId,
        @NotBlank String title,
        @NotEmpty List<LineItem> items,
        @NotNull Instant dueDate,
        String idempotencyKey
) {
    public IssueTuitionInvoiceRequest withIdempotencyKey(String key) {
        return new IssueTuitionInvoiceRequest(studentId, guardianId, classEnrollmentId, title, items, dueDate, key);
    }
}
