package com.lumie.attendance.domain.repository;

import com.lumie.attendance.domain.entity.AttendanceSession;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long>,
        JpaSpecificationExecutor<AttendanceSession> {

    Optional<AttendanceSession> findByAttendanceCodeAndSessionDate(String attendanceCode, LocalDate sessionDate);

    boolean existsByClassIdAndSessionDate(Long classId, LocalDate sessionDate);

    Optional<AttendanceSession> findByClassIdAndSessionDate(Long classId, LocalDate sessionDate);

    List<AttendanceSession> findByClassIdAndSessionDateBetween(Long classId, LocalDate from, LocalDate to);

    long countBySessionDateBetween(LocalDate from, LocalDate to);

    /**
     * Finds today's session whose attendance code matches. The session_date guard
     * prevents stale codes from yesterday from matching. "Today" is resolved in KST
     * so check-in agrees with the KST session_date set at on-demand session creation
     * (a UTC default would mismatch during the 00:00–09:00 KST window).
     */
    default Optional<AttendanceSession> findByAttendanceCode(String code) {
        return findByAttendanceCodeAndSessionDate(code, LocalDate.now(ZoneId.of("Asia/Seoul")));
    }

    default Page<AttendanceSession> search(Long classId, LocalDate dateFrom,
                                           LocalDate dateTo, Pageable pageable) {
        Specification<AttendanceSession> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (classId != null) {
                predicates.add(cb.equal(root.get("classId"), classId));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("sessionDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("sessionDate"), dateTo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
