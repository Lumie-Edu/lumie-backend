package com.lumie.admin.application.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record PayrollRequest(
    @NotNull(message = "Admin ID is required")
    Long adminId,

    @NotNull(message = "Year is required")
    Integer year,

    @NotNull(message = "Month is required")
    Integer month,

    BigDecimal baseSalary,
    Integer totalWorkDays,
    Integer actualWorkDays,
    Integer totalOvertimeMinutes,
    String note,
    List<PayrollItemRequest> items
) {
    public record PayrollItemRequest(
        String type,
        String name,
        BigDecimal amount,
        String note
    ) {}
}
