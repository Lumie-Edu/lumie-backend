package com.lumie.classroom.application.port.out;

import com.lumie.classroom.domain.entity.ClassEnrollment;
import com.lumie.classroom.domain.vo.EnrollmentStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClassEnrollmentPersistencePort {
    Page<ClassEnrollment> findByClassEntityId(Long classId, Pageable pageable);
    List<ClassEnrollment> findByStudentId(Long studentId);
    List<ClassEnrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);
    List<ClassEnrollment> findByStudentIdInAndStatus(Collection<Long> studentIds, EnrollmentStatus status);
    Page<ClassEnrollment> findByStudentId(Long studentId, Pageable pageable);
    Page<ClassEnrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status, Pageable pageable);
    List<ClassEnrollment> findByClassEntityIdAndStatus(Long classId, EnrollmentStatus status);
    List<Long> findStudentIdsByStatus(EnrollmentStatus status);
    long countByClassEntityIdAndStatus(Long classId, EnrollmentStatus status);
    boolean existsByClassEntityIdAndStudentId(Long classId, Long studentId);
    boolean existsByClassEntityIdAndStudentIdAndStatus(Long classId, Long studentId, EnrollmentStatus status);
    boolean existsByStudentIdAndStatus(Long studentId, EnrollmentStatus status);
    List<ClassEnrollment> findAllByStudentIdAndStatus(Long studentId, EnrollmentStatus status);
    Optional<ClassEnrollment> findByClassEntityIdAndStudentId(Long classId, Long studentId);
    Optional<ClassEnrollment> findById(Long enrollmentId);
    ClassEnrollment save(ClassEnrollment enrollment);
    List<ClassEnrollment> saveAll(Iterable<ClassEnrollment> enrollments);
    void delete(ClassEnrollment enrollment);
}
