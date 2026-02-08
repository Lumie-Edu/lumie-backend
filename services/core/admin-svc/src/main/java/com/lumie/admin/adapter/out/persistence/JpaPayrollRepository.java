package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.PayrollRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaPayrollRepository extends JpaRepository<PayrollRecord, Long> {
    Optional<PayrollRecord> findByAdminIdAndPayYearAndPayMonth(Long adminId, int year, int month);

    @Query("SELECT pr FROM PayrollRecord pr WHERE " +
           "(:year IS NULL OR pr.payYear = :year) AND " +
           "(:month IS NULL OR pr.payMonth = :month) AND " +
           "(:adminId IS NULL OR pr.admin.id = :adminId) AND " +
           "(:status IS NULL OR pr.status = :status)")
    Page<PayrollRecord> findByFilters(@Param("year") Integer year,
                                       @Param("month") Integer month,
                                       @Param("adminId") Long adminId,
                                       @Param("status") com.lumie.admin.domain.vo.PayrollStatus status,
                                       Pageable pageable);
}
