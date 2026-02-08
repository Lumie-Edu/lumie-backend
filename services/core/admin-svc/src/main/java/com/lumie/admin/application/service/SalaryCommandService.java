package com.lumie.admin.application.service;

import com.lumie.admin.application.dto.SalaryRequest;
import com.lumie.admin.application.dto.SalaryResponse;
import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.entity.Salary;
import com.lumie.admin.domain.exception.AdminNotFoundException;
import com.lumie.admin.domain.repository.AdminRepository;
import com.lumie.admin.domain.repository.SalaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SalaryCommandService {

    private final SalaryRepository salaryRepository;
    private final AdminRepository adminRepository;

    public SalaryResponse setSalary(Long adminId, SalaryRequest request) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new AdminNotFoundException(adminId));

        // End current salary if exists
        Optional<Salary> currentSalary = salaryRepository.findCurrentByAdminId(adminId, LocalDate.now());
        currentSalary.ifPresent(salary -> {
            salary.endEffectiveness(request.effectiveFrom().minusDays(1));
            salaryRepository.save(salary);
        });

        Salary salary = Salary.builder()
                .admin(admin)
                .baseSalary(request.baseSalary())
                .effectiveFrom(request.effectiveFrom())
                .effectiveTo(request.effectiveTo())
                .note(request.note())
                .build();

        Salary saved = salaryRepository.save(salary);
        log.info("Salary set for admin {}: {}", adminId, request.baseSalary());
        return SalaryResponse.from(saved);
    }
}
