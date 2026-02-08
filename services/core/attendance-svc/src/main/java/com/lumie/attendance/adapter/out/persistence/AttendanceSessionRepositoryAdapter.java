package com.lumie.attendance.adapter.out.persistence;

import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.repository.AttendanceSessionRepository;
import com.lumie.attendance.domain.vo.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AttendanceSessionRepositoryAdapter implements AttendanceSessionRepository {

    private final JpaAttendanceSessionRepository jpaRepository;

    @Override
    public AttendanceSession save(AttendanceSession session) {
        return jpaRepository.save(session);
    }

    @Override
    public Optional<AttendanceSession> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<AttendanceSession> findByAcademyId(Long academyId, Pageable pageable) {
        return jpaRepository.findByAcademyId(academyId, pageable);
    }

    @Override
    public Optional<AttendanceSession> findByAttendanceCode(String code) {
        return jpaRepository.findByAttendanceCodeAndStatus(code, SessionStatus.OPEN);
    }

    @Override
    public Page<AttendanceSession> search(Long academyId, LocalDate dateFrom, LocalDate dateTo,
                                          SessionStatus status, Pageable pageable) {
        return jpaRepository.search(academyId, dateFrom, dateTo, status, pageable);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
