package com.lumie.attendance.adapter.out.persistence;

import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.repository.AttendanceSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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
        return jpaRepository.findByAttendanceCodeAndSessionDate(code, LocalDate.now());
    }

    @Override
    public Page<AttendanceSession> search(Long academyId, Long classId, LocalDate dateFrom,
                                          LocalDate dateTo, Pageable pageable) {
        Specification<AttendanceSession> spec = buildSearchSpec(academyId, classId, dateFrom, dateTo);
        return jpaRepository.findAll(spec, pageable);
    }

    private Specification<AttendanceSession> buildSearchSpec(Long academyId, Long classId,
                                                             LocalDate dateFrom, LocalDate dateTo) {
        Specification<AttendanceSession> spec = Specification.where(null);

        if (academyId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("academyId"), academyId));
        }
        if (classId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("classId"), classId));
        }
        if (dateFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("sessionDate"), dateFrom));
        }
        if (dateTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("sessionDate"), dateTo));
        }

        return spec;
    }

    @Override
    public boolean existsByClassIdAndSessionDate(Long classId, LocalDate sessionDate) {
        return jpaRepository.existsByClassIdAndSessionDate(classId, sessionDate);
    }

    @Override
    public List<AttendanceSession> findByClassIdAndSessionDateBetween(Long classId, LocalDate from, LocalDate to) {
        return jpaRepository.findByClassIdAndSessionDateBetween(classId, from, to);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public long countBySessionDateBetween(LocalDate from, LocalDate to) {
        return jpaRepository.countBySessionDateBetween(from, to);
    }
}
