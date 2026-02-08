package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface JpaWorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {
    List<WorkSchedule> findByAdminId(Long adminId);

    @Query("SELECT ws FROM WorkSchedule ws WHERE ws.admin.id = :adminId " +
           "AND ws.effectiveFrom <= :date AND (ws.effectiveTo IS NULL OR ws.effectiveTo >= :date)")
    List<WorkSchedule> findByAdminIdAndEffectiveDate(@Param("adminId") Long adminId,
                                                      @Param("date") LocalDate date);
}
