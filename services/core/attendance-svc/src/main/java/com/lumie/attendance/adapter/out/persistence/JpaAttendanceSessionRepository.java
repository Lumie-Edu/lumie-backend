package com.lumie.attendance.adapter.out.persistence;

import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.vo.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface JpaAttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    Page<AttendanceSession> findByAcademyId(Long academyId, Pageable pageable);

    Optional<AttendanceSession> findByAttendanceCodeAndStatus(String attendanceCode, SessionStatus status);

    @Query("SELECT s FROM AttendanceSession s " +
           "WHERE (:academyId IS NULL OR s.academyId = :academyId) " +
           "AND (:dateFrom IS NULL OR s.sessionDate >= :dateFrom) " +
           "AND (:dateTo IS NULL OR s.sessionDate <= :dateTo) " +
           "AND (:status IS NULL OR s.status = :status)")
    Page<AttendanceSession> search(
        @Param("academyId") Long academyId,
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo,
        @Param("status") SessionStatus status,
        Pageable pageable
    );
}
