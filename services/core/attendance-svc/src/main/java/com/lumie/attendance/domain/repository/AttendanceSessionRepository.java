package com.lumie.attendance.domain.repository;

import com.lumie.attendance.domain.entity.AttendanceSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


public interface AttendanceSessionRepository {

    AttendanceSession save(AttendanceSession session);

    Optional<AttendanceSession> findById(Long id);

    Page<AttendanceSession> findByAcademyId(Long academyId, Pageable pageable);

    Optional<AttendanceSession> findByAttendanceCode(String code);

    Page<AttendanceSession> search(Long academyId, Long classId, LocalDate dateFrom,
                                   LocalDate dateTo, Pageable pageable);

    boolean existsByClassIdAndSessionDate(Long classId, LocalDate sessionDate);

    List<AttendanceSession> findByClassIdAndSessionDateBetween(Long classId, LocalDate from, LocalDate to);

    void deleteById(Long id);

    long countBySessionDateBetween(LocalDate from, LocalDate to);
}
