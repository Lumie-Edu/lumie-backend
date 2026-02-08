package com.lumie.admin.domain.entity;

import com.lumie.admin.domain.vo.PayrollItemType;
import com.lumie.admin.domain.vo.PayrollStatus;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payroll_records",
    uniqueConstraints = @UniqueConstraint(columnNames = {"admin_id", "pay_year", "pay_month"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayrollRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Column(name = "pay_year", nullable = false)
    private Integer payYear;

    @Column(name = "pay_month", nullable = false)
    private Integer payMonth;

    @Column(name = "base_salary", nullable = false, precision = 12, scale = 0)
    private BigDecimal baseSalary;

    @Column(name = "total_allowances", nullable = false, precision = 12, scale = 0)
    private BigDecimal totalAllowances = BigDecimal.ZERO;

    @Column(name = "total_deductions", nullable = false, precision = 12, scale = 0)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "net_pay", nullable = false, precision = 12, scale = 0)
    private BigDecimal netPay;

    @Column(name = "total_work_days")
    private Integer totalWorkDays;

    @Column(name = "actual_work_days")
    private Integer actualWorkDays;

    @Column(name = "total_overtime_minutes")
    private Integer totalOvertimeMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayrollStatus status = PayrollStatus.DRAFT;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "payrollRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayrollItem> items = new ArrayList<>();

    @Builder
    private PayrollRecord(Admin admin, Integer payYear, Integer payMonth, BigDecimal baseSalary,
                          Integer totalWorkDays, Integer actualWorkDays, Integer totalOvertimeMinutes,
                          String note) {
        this.admin = admin;
        this.payYear = payYear;
        this.payMonth = payMonth;
        this.baseSalary = baseSalary;
        this.totalWorkDays = totalWorkDays;
        this.actualWorkDays = actualWorkDays;
        this.totalOvertimeMinutes = totalOvertimeMinutes != null ? totalOvertimeMinutes : 0;
        this.note = note;
        this.totalAllowances = BigDecimal.ZERO;
        this.totalDeductions = BigDecimal.ZERO;
        this.netPay = baseSalary;
        this.status = PayrollStatus.DRAFT;
    }

    public void addItem(PayrollItem item) {
        items.add(item);
        recalculate();
    }

    public void calculateNetPay() {
        recalculate();
    }

    private void recalculate() {
        this.totalAllowances = items.stream()
                .filter(i -> i.getItemType() == PayrollItemType.ALLOWANCE)
                .map(PayrollItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalDeductions = items.stream()
                .filter(i -> i.getItemType() == PayrollItemType.DEDUCTION)
                .map(PayrollItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.netPay = baseSalary.add(totalAllowances).subtract(totalDeductions);
    }

    public void finalize_() {
        recalculate();
        this.status = PayrollStatus.FINALIZED;
    }

    public void markPaid() {
        if (this.status != PayrollStatus.FINALIZED) {
            throw new IllegalStateException("Payroll must be finalized before marking as paid");
        }
        this.status = PayrollStatus.PAID;
    }

    public void update(BigDecimal baseSalary, Integer totalWorkDays, Integer actualWorkDays,
                       Integer totalOvertimeMinutes, String note) {
        if (this.status != PayrollStatus.DRAFT) {
            throw new IllegalStateException("Cannot update non-draft payroll");
        }
        if (baseSalary != null) this.baseSalary = baseSalary;
        if (totalWorkDays != null) this.totalWorkDays = totalWorkDays;
        if (actualWorkDays != null) this.actualWorkDays = actualWorkDays;
        if (totalOvertimeMinutes != null) this.totalOvertimeMinutes = totalOvertimeMinutes;
        if (note != null) this.note = note;
        recalculate();
    }
}
