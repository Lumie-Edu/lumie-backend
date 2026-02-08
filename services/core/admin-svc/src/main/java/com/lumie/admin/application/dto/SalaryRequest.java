package com.lumie.admin.application.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalaryRequest(
    @NotNull(message = "Base salary is required")
    BigDecimal baseSalary,

    @NotNull(message = "Effective from date is required")
    LocalDate effectiveFrom,

    LocalDate effectiveTo,
    String note
) {
}
