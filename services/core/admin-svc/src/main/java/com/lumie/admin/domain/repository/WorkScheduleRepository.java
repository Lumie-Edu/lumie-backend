package com.lumie.admin.domain.repository;

import com.lumie.admin.domain.entity.WorkSchedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkScheduleRepository {
    WorkSchedule save(WorkSchedule workSchedule);
    Optional<WorkSchedule> findById(Long id);
    List<WorkSchedule> findByAdminId(Long adminId);
    List<WorkSchedule> findByAdminIdAndEffectiveDate(Long adminId, LocalDate date);
    void delete(WorkSchedule workSchedule);
    void deleteAll(List<WorkSchedule> schedules);
}
