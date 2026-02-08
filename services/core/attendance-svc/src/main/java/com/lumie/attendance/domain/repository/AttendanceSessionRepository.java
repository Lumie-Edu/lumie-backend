package com.lumie.attendance.domain.repository;

import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.vo.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface AttendanceSessionRepository {

    AttendanceSession save(AttendanceSession session);

    Optional<AttendanceSession> findById(Long id);

    Page<AttendanceSession> findByAcademyId(Long academyId, Pageable pageable);

    Optional<AttendanceSession> findByAttendanceCode(String code);

    Page<AttendanceSession> search(Long academyId, LocalDate dateFrom, LocalDate dateTo,
                                   SessionStatus status, Pageable pageable);

    void deleteById(Long id);
}
