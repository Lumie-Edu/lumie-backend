package com.lumie.admin.domain.repository;

import com.lumie.admin.domain.entity.Attendance;
import com.lumie.admin.domain.vo.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository {
    Attendance save(Attendance attendance);
    Optional<Attendance> findById(Long id);
    Optional<Attendance> findByAdminIdAndWorkDate(Long adminId, LocalDate workDate);
    Page<Attendance> findByFilters(Long adminId, LocalDate dateFrom, LocalDate dateTo,
                                   AttendanceStatus status, Pageable pageable);
    List<Attendance> findByAdminIdAndWorkDateBetween(Long adminId, LocalDate from, LocalDate to);
}
