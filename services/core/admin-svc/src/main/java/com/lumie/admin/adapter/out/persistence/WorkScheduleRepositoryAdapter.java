package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.WorkSchedule;
import com.lumie.admin.domain.repository.WorkScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WorkScheduleRepositoryAdapter implements WorkScheduleRepository {
    private final JpaWorkScheduleRepository jpaWorkScheduleRepository;

    @Override
    public WorkSchedule save(WorkSchedule workSchedule) {
        return jpaWorkScheduleRepository.save(workSchedule);
    }

    @Override
    public Optional<WorkSchedule> findById(Long id) {
        return jpaWorkScheduleRepository.findById(id);
    }

    @Override
    public List<WorkSchedule> findByAdminId(Long adminId) {
        return jpaWorkScheduleRepository.findByAdminId(adminId);
    }

    @Override
    public List<WorkSchedule> findByAdminIdAndEffectiveDate(Long adminId, LocalDate date) {
        return jpaWorkScheduleRepository.findByAdminIdAndEffectiveDate(adminId, date);
    }

    @Override
    public void delete(WorkSchedule workSchedule) {
        jpaWorkScheduleRepository.delete(workSchedule);
    }

    @Override
    public void deleteAll(List<WorkSchedule> schedules) {
        jpaWorkScheduleRepository.deleteAll(schedules);
    }
}
