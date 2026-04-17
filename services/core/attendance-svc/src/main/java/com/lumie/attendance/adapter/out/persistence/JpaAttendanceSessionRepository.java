package com.lumie.attendance.adapter.out.persistence;

import com.lumie.attendance.domain.entity.AttendanceSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaAttendanceSessionRepository extends JpaRepository<AttendanceSession, Long>,
        JpaSpecificationExecutor<AttendanceSession> {

    Page<AttendanceSession> findByAcademyId(Long academyId, Pageable pageable);

    Optional<AttendanceSession> findByAttendanceCodeAndSessionDate(String attendanceCode, LocalDate sessionDate);

    boolean existsByClassIdAndSessionDate(Long classId, LocalDate sessionDate);

    List<AttendanceSession> findByClassIdAndSessionDateBetween(Long classId, LocalDate from, LocalDate to);
}
