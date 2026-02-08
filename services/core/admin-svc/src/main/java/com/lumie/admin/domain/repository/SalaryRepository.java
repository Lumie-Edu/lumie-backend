package com.lumie.admin.domain.repository;

import com.lumie.admin.domain.entity.Salary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalaryRepository {
    Salary save(Salary salary);
    Optional<Salary> findById(Long id);
    List<Salary> findByAdminId(Long adminId);
    Optional<Salary> findCurrentByAdminId(Long adminId, LocalDate date);
}
