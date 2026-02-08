package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaSalaryRepository extends JpaRepository<Salary, Long> {
    List<Salary> findByAdminIdOrderByEffectiveFromDesc(Long adminId);

    @Query("SELECT s FROM Salary s WHERE s.admin.id = :adminId " +
           "AND s.effectiveFrom <= :date AND (s.effectiveTo IS NULL OR s.effectiveTo >= :date)")
    Optional<Salary> findCurrentByAdminId(@Param("adminId") Long adminId, @Param("date") LocalDate date);
}
