package com.lumie.attendance.adapter.out.persistence;

import com.lumie.attendance.domain.entity.AttendanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaAttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findBySessionId(Long sessionId);

    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    Page<AttendanceRecord> findByStudentId(Long studentId, Pageable pageable);

    @Query("SELECT r FROM AttendanceRecord r JOIN FETCH r.session WHERE r.studentId = :studentId ORDER BY r.createdAt DESC")
    List<AttendanceRecord> findByStudentIdWithSession(@Param("studentId") Long studentId);
}
