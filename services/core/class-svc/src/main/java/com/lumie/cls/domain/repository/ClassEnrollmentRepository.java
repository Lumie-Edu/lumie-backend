package com.lumie.cls.domain.repository;

import com.lumie.cls.domain.entity.ClassEnrollment;
import com.lumie.cls.domain.vo.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ClassEnrollmentRepository {

    ClassEnrollment save(ClassEnrollment enrollment);

    List<ClassEnrollment> saveAll(List<ClassEnrollment> enrollments);

    Optional<ClassEnrollment> findById(Long id);

    Page<ClassEnrollment> findByClassEntityId(Long classId, Pageable pageable);

    List<ClassEnrollment> findByStudentId(Long studentId);

    Page<ClassEnrollment> findByStudentId(Long studentId, Pageable pageable);

    long countByClassEntityIdAndStatus(Long classId, EnrollmentStatus status);

    boolean existsByClassEntityIdAndStudentId(Long classId, Long studentId);

    Optional<ClassEnrollment> findByClassEntityIdAndStudentId(Long classId, Long studentId);
}
