package com.lumie.admin.application.service;

import com.lumie.admin.application.dto.PayrollResponse;
import com.lumie.admin.domain.exception.PayrollNotFoundException;
import com.lumie.admin.domain.repository.PayrollRepository;
import com.lumie.admin.domain.vo.PayrollStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayrollQueryService {

    private final PayrollRepository payrollRepository;

    public PayrollResponse getPayroll(Long id) {
        return payrollRepository.findById(id)
                .map(PayrollResponse::from)
                .orElseThrow(() -> new PayrollNotFoundException(id));
    }

    public Page<PayrollResponse> getPayrolls(Integer year, Integer month, Long adminId,
                                              PayrollStatus status, Pageable pageable) {
        return payrollRepository.findByFilters(year, month, adminId, status, pageable)
                .map(PayrollResponse::from);
    }
}
