package com.lumie.admin.domain.repository;

import com.lumie.admin.domain.entity.PayrollRecord;
import com.lumie.admin.domain.vo.PayrollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PayrollRepository {
    PayrollRecord save(PayrollRecord payrollRecord);
    Optional<PayrollRecord> findById(Long id);
    Optional<PayrollRecord> findByAdminIdAndPayYearAndPayMonth(Long adminId, int year, int month);
    Page<PayrollRecord> findByFilters(Integer year, Integer month, Long adminId,
                                       PayrollStatus status, Pageable pageable);
}
