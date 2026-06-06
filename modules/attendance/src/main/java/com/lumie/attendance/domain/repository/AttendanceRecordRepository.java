package com.lumie.attendance.domain.repository;

import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.vo.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findBySessionId(Long sessionId);

    List<AttendanceRecord> findBySessionIdIn(Collection<Long> sessionIds);

    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    Page<AttendanceRecord> findByStudentId(Long studentId, Pageable pageable);

    @Query("SELECT r FROM AttendanceRecord r JOIN FETCH r.session WHERE r.studentId = :studentId ORDER BY r.createdAt DESC")
    List<AttendanceRecord> findByStudentIdWithSession(@Param("studentId") Long studentId);

    @Query("""
        SELECT r.session.id AS sessionId, r.status AS status, COUNT(r) AS recordCount
        FROM AttendanceRecord r
        WHERE r.session.id IN :sessionIds
        GROUP BY r.session.id, r.status
        """)
    List<SessionStatusCount> countBySessionIdsGroupedByStatus(@Param("sessionIds") Collection<Long> sessionIds);

    long countBySessionSessionDateBetween(LocalDate from, LocalDate to);

    long countBySessionSessionDateBetweenAndStatus(LocalDate from, LocalDate to, AttendanceStatus status);

    long countBySessionSessionDateBetweenAndStatusNot(LocalDate from, LocalDate to, AttendanceStatus status);

    interface SessionStatusCount {
        Long getSessionId();
        AttendanceStatus getStatus();
        long getRecordCount();
    }
}
