package com.lumie.admin.adapter.out.persistence;

import com.lumie.admin.domain.entity.Attendance;
import com.lumie.admin.domain.repository.AttendanceRepository;
import com.lumie.admin.domain.vo.AttendanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AttendanceRepositoryAdapter implements AttendanceRepository {
    private final JpaAttendanceRepository jpaAttendanceRepository;

    @Override
    public Attendance save(Attendance attendance) {
        return jpaAttendanceRepository.save(attendance);
    }

    @Override
    public Optional<Attendance> findById(Long id) {
        return jpaAttendanceRepository.findById(id);
    }

    @Override
    public Optional<Attendance> findByAdminIdAndWorkDate(Long adminId, LocalDate workDate) {
        return jpaAttendanceRepository.findByAdminIdAndWorkDate(adminId, workDate);
    }

    @Override
    public Page<Attendance> findByFilters(Long adminId, LocalDate dateFrom, LocalDate dateTo,
                                           AttendanceStatus status, Pageable pageable) {
        return jpaAttendanceRepository.findByFilters(adminId, dateFrom, dateTo, status, pageable);
    }

    @Override
    public List<Attendance> findByAdminIdAndWorkDateBetween(Long adminId, LocalDate from, LocalDate to) {
        return jpaAttendanceRepository.findByAdminIdAndWorkDateBetween(adminId, from, to);
    }
}
