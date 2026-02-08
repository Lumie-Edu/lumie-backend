package com.lumie.admin.domain.entity;

import com.lumie.admin.domain.vo.PayrollItemType;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "payroll_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayrollItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_record_id", nullable = false)
    private PayrollRecord payrollRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private PayrollItemType itemType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Column(name = "note", length = 255)
    private String note;

    @Builder
    private PayrollItem(PayrollRecord payrollRecord, PayrollItemType itemType,
                        String name, BigDecimal amount, String note) {
        this.payrollRecord = payrollRecord;
        this.itemType = itemType;
        this.name = name;
        this.amount = amount;
        this.note = note;
    }

    public static PayrollItem createAllowance(PayrollRecord payrollRecord, String name,
                                               BigDecimal amount, String note) {
        return PayrollItem.builder()
                .payrollRecord(payrollRecord)
                .itemType(PayrollItemType.ALLOWANCE)
                .name(name)
                .amount(amount)
                .note(note)
                .build();
    }

    public static PayrollItem createDeduction(PayrollRecord payrollRecord, String name,
                                               BigDecimal amount, String note) {
        return PayrollItem.builder()
                .payrollRecord(payrollRecord)
                .itemType(PayrollItemType.DEDUCTION)
                .name(name)
                .amount(amount)
                .note(note)
                .build();
    }
}
