package com.lumie.cls.adapter.out.persistence;

import com.lumie.cls.domain.entity.ClassEnrollment;
import com.lumie.cls.domain.vo.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    Page<ClassEnrollment> findByClassEntityId(Long classEntityId, Pageable pageable);

    List<ClassEnrollment> findByStudentId(Long studentId);

    List<ClassEnrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    Page<ClassEnrollment> findByStudentId(Long studentId, Pageable pageable);

    Page<ClassEnrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status, Pageable pageable);

    List<ClassEnrollment> findByClassEntityIdAndStatus(Long classEntityId, EnrollmentStatus status);

    long countByClassEntityIdAndStatus(Long classEntityId, EnrollmentStatus status);

    boolean existsByClassEntityIdAndStudentId(Long classEntityId, Long studentId);

    Optional<ClassEnrollment> findByClassEntityIdAndStudentId(Long classEntityId, Long studentId);
}
