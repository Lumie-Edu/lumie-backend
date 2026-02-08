package com.lumie.admin.application.dto;

import com.lumie.admin.domain.entity.PayrollItem;
import com.lumie.admin.domain.entity.PayrollRecord;
import com.lumie.admin.domain.vo.PayrollStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PayrollResponse(
    Long id,
    Long adminId,
    String adminName,
    Integer payYear,
    Integer payMonth,
    BigDecimal baseSalary,
    BigDecimal totalAllowances,
    BigDecimal totalDeductions,
    BigDecimal netPay,
    Integer totalWorkDays,
    Integer actualWorkDays,
    Integer totalOvertimeMinutes,
    PayrollStatus status,
    String note,
    List<PayrollItemResponse> items,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public record PayrollItemResponse(
        Long id,
        String itemType,
        String name,
        BigDecimal amount,
        String note
    ) {
        public static PayrollItemResponse from(PayrollItem item) {
            return new PayrollItemResponse(
                item.getId(),
                item.getItemType().name(),
                item.getName(),
                item.getAmount(),
                item.getNote()
            );
        }
    }

    public static PayrollResponse from(PayrollRecord record) {
        List<PayrollItemResponse> itemResponses = record.getItems().stream()
            .map(PayrollItemResponse::from)
            .toList();

        return new PayrollResponse(
            record.getId(),
            record.getAdmin().getId(),
            record.getAdmin().getAdminName(),
            record.getPayYear(),
            record.getPayMonth(),
            record.getBaseSalary(),
            record.getTotalAllowances(),
            record.getTotalDeductions(),
            record.getNetPay(),
            record.getTotalWorkDays(),
            record.getActualWorkDays(),
            record.getTotalOvertimeMinutes(),
            record.getStatus(),
            record.getNote(),
            itemResponses,
            record.getCreatedAt(),
            record.getUpdatedAt()
        );
    }
}
