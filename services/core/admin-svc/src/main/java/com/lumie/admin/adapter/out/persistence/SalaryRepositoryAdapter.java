package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.Salary;
import com.lumie.admin.domain.repository.SalaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SalaryRepositoryAdapter implements SalaryRepository {
    private final JpaSalaryRepository jpaSalaryRepository;

    @Override
    public Salary save(Salary salary) {
        return jpaSalaryRepository.save(salary);
    }

    @Override
    public Optional<Salary> findById(Long id) {
        return jpaSalaryRepository.findById(id);
    }

    @Override
    public List<Salary> findByAdminId(Long adminId) {
        return jpaSalaryRepository.findByAdminIdOrderByEffectiveFromDesc(adminId);
    }

    @Override
    public Optional<Salary> findCurrentByAdminId(Long adminId, LocalDate date) {
        return jpaSalaryRepository.findCurrentByAdminId(adminId, date);
    }
}
