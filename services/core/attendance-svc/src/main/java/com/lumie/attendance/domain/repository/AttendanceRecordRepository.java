package com.lumie.attendance.domain.repository;

import com.lumie.attendance.domain.entity.AttendanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository {

    AttendanceRecord save(AttendanceRecord record);

    List<AttendanceRecord> saveAll(List<AttendanceRecord> records);

    Optional<AttendanceRecord> findById(Long id);

    List<AttendanceRecord> findBySessionId(Long sessionId);

    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    Page<AttendanceRecord> findByStudentId(Long studentId, Pageable pageable);

    List<AttendanceRecord> findAllById(List<Long> ids);
}
