package com.lumie.classroom.domain.repository;

import com.lumie.classroom.domain.entity.ClassEnrollment;
import com.lumie.classroom.domain.vo.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    Page<ClassEnrollment> findByClassEntityId(Long classEntityId, Pageable pageable);

    List<ClassEnrollment> findByStudentId(Long studentId);

    List<ClassEnrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    @EntityGraph(attributePaths = "classEntity")
    List<ClassEnrollment> findByStudentIdInAndStatus(Collection<Long> studentIds, EnrollmentStatus status);

    Page<ClassEnrollment> findByStudentId(Long studentId, Pageable pageable);

    Page<ClassEnrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status, Pageable pageable);

    List<ClassEnrollment> findByClassEntityIdAndStatus(Long classEntityId, EnrollmentStatus status);

    @Query("SELECT DISTINCT enrollment.studentId FROM ClassEnrollment enrollment "
            + "WHERE enrollment.status = :status "
            + "AND enrollment.studentId IS NOT NULL")
    List<Long> findStudentIdsByStatus(@Param("status") EnrollmentStatus status);

    long countByClassEntityIdAndStatus(Long classEntityId, EnrollmentStatus status);

    boolean existsByClassEntityIdAndStudentId(Long classEntityId, Long studentId);

    boolean existsByClassEntityIdAndStudentIdAndStatus(
            Long classEntityId,
            Long studentId,
            EnrollmentStatus status
    );

    boolean existsByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    List<ClassEnrollment> findAllByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    Optional<ClassEnrollment> findByClassEntityIdAndStudentId(Long classEntityId, Long studentId);
}
