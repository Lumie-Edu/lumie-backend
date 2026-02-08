package com.lumie.admin.application.dto;

import com.lumie.admin.domain.entity.Salary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SalaryResponse(
    Long id,
    Long adminId,
    BigDecimal baseSalary,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    String note,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static SalaryResponse from(Salary salary) {
        return new SalaryResponse(
            salary.getId(),
            salary.getAdmin().getId(),
            salary.getBaseSalary(),
            salary.getEffectiveFrom(),
            salary.getEffectiveTo(),
            salary.getNote(),
            salary.getCreatedAt(),
            salary.getUpdatedAt()
        );
    }
}
