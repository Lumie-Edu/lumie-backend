package com.lumie.attendance.adapter.out.persistence;

import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.repository.AttendanceRecordRepository;
import com.lumie.attendance.domain.vo.AttendanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AttendanceRecordRepositoryAdapter implements AttendanceRecordRepository {

    private final JpaAttendanceRecordRepository jpaRepository;

    @Override
    public AttendanceRecord save(AttendanceRecord record) {
        return jpaRepository.save(record);
    }

    @Override
    public List<AttendanceRecord> saveAll(List<AttendanceRecord> records) {
        return jpaRepository.saveAll(records);
    }

    @Override
    public Optional<AttendanceRecord> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<AttendanceRecord> findBySessionId(Long sessionId) {
        return jpaRepository.findBySessionId(sessionId);
    }

    @Override
    public Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId) {
        return jpaRepository.findBySessionIdAndStudentId(sessionId, studentId);
    }

    @Override
    public Page<AttendanceRecord> findByStudentId(Long studentId, Pageable pageable) {
        return jpaRepository.findByStudentId(studentId, pageable);
    }

    @Override
    public List<AttendanceRecord> findAllById(List<Long> ids) {
        return jpaRepository.findAllById(ids);
    }

    @Override
    public List<AttendanceRecord> findByStudentIdWithSession(Long studentId) {
        return jpaRepository.findByStudentIdWithSession(studentId);
    }

    @Override
    public long countBySessionSessionDateBetween(LocalDate from, LocalDate to) {
        return jpaRepository.countBySessionSessionDateBetween(from, to);
    }

    @Override
    public long countBySessionSessionDateBetweenAndStatus(LocalDate from, LocalDate to, AttendanceStatus status) {
        return jpaRepository.countBySessionSessionDateBetweenAndStatus(from, to, status);
    }
}
