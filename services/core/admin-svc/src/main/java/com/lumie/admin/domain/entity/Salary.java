package com.lumie.admin.domain.entity;

import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "salaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Salary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Column(name = "base_salary", nullable = false, precision = 12, scale = 0)
    private BigDecimal baseSalary;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "note", length = 500)
    private String note;

    @Builder
    private Salary(Admin admin, BigDecimal baseSalary, LocalDate effectiveFrom,
                   LocalDate effectiveTo, String note) {
        this.admin = admin;
        this.baseSalary = baseSalary;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.note = note;
    }

    public void endEffectiveness(LocalDate endDate) {
        this.effectiveTo = endDate;
    }

    public boolean isCurrentlyEffective() {
        LocalDate now = LocalDate.now();
        if (now.isBefore(effectiveFrom)) {
            return false;
        }
        return effectiveTo == null || !now.isAfter(effectiveTo);
    }
}
