package com.lumie.admin.application.service;

import com.lumie.admin.application.dto.SalaryResponse;
import com.lumie.admin.domain.exception.SalaryNotFoundException;
import com.lumie.admin.domain.repository.SalaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalaryQueryService {

    private final SalaryRepository salaryRepository;

    public SalaryResponse getCurrentSalary(Long adminId) {
        return salaryRepository.findCurrentByAdminId(adminId, LocalDate.now())
                .map(SalaryResponse::from)
                .orElseThrow(() -> new SalaryNotFoundException(adminId));
    }

    public List<SalaryResponse> getSalaryHistory(Long adminId) {
        return salaryRepository.findByAdminId(adminId).stream()
                .map(SalaryResponse::from)
                .toList();
    }
}
