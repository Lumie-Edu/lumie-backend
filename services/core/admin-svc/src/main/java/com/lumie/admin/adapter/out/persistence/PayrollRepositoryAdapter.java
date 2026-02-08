package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.PayrollRecord;
import com.lumie.admin.domain.repository.PayrollRepository;
import com.lumie.admin.domain.vo.PayrollStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PayrollRepositoryAdapter implements PayrollRepository {
    private final JpaPayrollRepository jpaPayrollRepository;

    @Override
    public PayrollRecord save(PayrollRecord payrollRecord) {
        return jpaPayrollRepository.save(payrollRecord);
    }

    @Override
    public Optional<PayrollRecord> findById(Long id) {
        return jpaPayrollRepository.findById(id);
    }

    @Override
    public Optional<PayrollRecord> findByAdminIdAndPayYearAndPayMonth(Long adminId, int year, int month) {
        return jpaPayrollRepository.findByAdminIdAndPayYearAndPayMonth(adminId, year, month);
    }

    @Override
    public Page<PayrollRecord> findByFilters(Integer year, Integer month, Long adminId,
                                              PayrollStatus status, Pageable pageable) {
        return jpaPayrollRepository.findByFilters(year, month, adminId, status, pageable);
    }
}
