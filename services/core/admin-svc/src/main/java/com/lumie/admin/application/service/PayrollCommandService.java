package com.lumie.admin.application.service;

import com.lumie.admin.application.dto.PayrollRequest;
import com.lumie.admin.application.dto.PayrollResponse;
import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.entity.PayrollItem;
import com.lumie.admin.domain.entity.PayrollRecord;
import com.lumie.admin.domain.entity.Salary;
import com.lumie.admin.domain.exception.AdminErrorCode;
import com.lumie.admin.domain.exception.AdminNotFoundException;
import com.lumie.admin.domain.exception.PayrollNotFoundException;
import com.lumie.admin.domain.exception.SalaryNotFoundException;
import com.lumie.admin.domain.repository.AdminRepository;
import com.lumie.admin.domain.repository.PayrollRepository;
import com.lumie.admin.domain.repository.SalaryRepository;
import com.lumie.admin.domain.vo.PayrollItemType;
import com.lumie.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PayrollCommandService {

    private final PayrollRepository payrollRepository;
    private final AdminRepository adminRepository;
    private final SalaryRepository salaryRepository;

    public PayrollResponse generateMonthlyPayroll(PayrollRequest request) {
        Admin admin = adminRepository.findById(request.adminId())
                .orElseThrow(() -> new AdminNotFoundException(request.adminId()));

        if (payrollRepository.findByAdminIdAndPayYearAndPayMonth(
                request.adminId(), request.year(), request.month()).isPresent()) {
            throw new BusinessException(AdminErrorCode.DUPLICATE_PAYROLL,
                    "Payroll already exists for this month");
        }

        BigDecimal baseSalary = request.baseSalary();
        if (baseSalary == null) {
            YearMonth ym = YearMonth.of(request.year(), request.month());
            Salary salary = salaryRepository.findCurrentByAdminId(request.adminId(), ym.atDay(1))
                    .orElseThrow(() -> new SalaryNotFoundException(request.adminId()));
            baseSalary = salary.getBaseSalary();
        }

        PayrollRecord record = PayrollRecord.builder()
                .admin(admin)
                .payYear(request.year())
                .payMonth(request.month())
                .baseSalary(baseSalary)
                .totalWorkDays(request.totalWorkDays())
                .actualWorkDays(request.actualWorkDays())
                .totalOvertimeMinutes(request.totalOvertimeMinutes())
                .note(request.note())
                .build();

        if (request.items() != null) {
            for (PayrollRequest.PayrollItemRequest itemReq : request.items()) {
                PayrollItemType type = PayrollItemType.valueOf(itemReq.type());
                PayrollItem item = PayrollItem.builder()
                        .payrollRecord(record)
                        .itemType(type)
                        .name(itemReq.name())
                        .amount(itemReq.amount())
                        .note(itemReq.note())
                        .build();
                record.addItem(item);
            }
        }

        PayrollRecord saved = payrollRepository.save(record);
        log.info("Payroll generated for admin {} - {}/{}", request.adminId(), request.year(), request.month());
        return PayrollResponse.from(saved);
    }

    public PayrollResponse updatePayroll(Long id, PayrollRequest request) {
        PayrollRecord record = payrollRepository.findById(id)
                .orElseThrow(() -> new PayrollNotFoundException(id));
        record.update(request.baseSalary(), request.totalWorkDays(), request.actualWorkDays(),
                request.totalOvertimeMinutes(), request.note());
        PayrollRecord updated = payrollRepository.save(record);
        log.info("Payroll {} updated", id);
        return PayrollResponse.from(updated);
    }

    public void finalizePayroll(Long id) {
        PayrollRecord record = payrollRepository.findById(id)
                .orElseThrow(() -> new PayrollNotFoundException(id));
        record.finalize_();
        payrollRepository.save(record);
        log.info("Payroll {} finalized", id);
    }

    public void markPaid(Long id) {
        PayrollRecord record = payrollRepository.findById(id)
                .orElseThrow(() -> new PayrollNotFoundException(id));
        record.markPaid();
        payrollRepository.save(record);
        log.info("Payroll {} marked as paid", id);
    }
}
