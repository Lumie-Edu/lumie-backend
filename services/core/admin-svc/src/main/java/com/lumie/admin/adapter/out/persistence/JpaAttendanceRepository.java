package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaAttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByAdminIdAndWorkDate(Long adminId, LocalDate workDate);

    List<Attendance> findByAdminIdAndWorkDateBetween(Long adminId, LocalDate from, LocalDate to);

    @Query("SELECT a FROM Attendance a WHERE " +
           "(:adminId IS NULL OR a.admin.id = :adminId) AND " +
           "(:dateFrom IS NULL OR a.workDate >= :dateFrom) AND " +
           "(:dateTo IS NULL OR a.workDate <= :dateTo) AND " +
           "(:status IS NULL OR a.status = :status)")
    Page<Attendance> findByFilters(@Param("adminId") Long adminId,
                                    @Param("dateFrom") LocalDate dateFrom,
                                    @Param("dateTo") LocalDate dateTo,
                                    @Param("status") com.lumie.admin.domain.vo.AttendanceStatus status,
                                    Pageable pageable);
}
