package com.lumie.classroom.adapter.out.persistence;

import com.lumie.classroom.application.port.out.ClassEnrollmentPersistencePort;
import com.lumie.classroom.domain.entity.ClassEnrollment;
import com.lumie.classroom.domain.vo.EnrollmentStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClassEnrollmentPersistenceAdapter implements ClassEnrollmentPersistencePort {
    private final ClassEnrollmentJpaRepository repository;

    @Override public Page<ClassEnrollment> findByClassEntityId(Long classId, Pageable pageable) {
        return repository.findByClassEntityId(classId, pageable);
    }
    @Override public List<ClassEnrollment> findByStudentId(Long studentId) {
        return repository.findByStudentId(studentId);
    }
    @Override public List<ClassEnrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status) {
        return repository.findByStudentIdAndStatus(studentId, status);
    }
    @Override public List<ClassEnrollment> findByStudentIdInAndStatus(Collection<Long> studentIds, EnrollmentStatus status) {
        return repository.findByStudentIdInAndStatus(studentIds, status);
    }
    @Override public Page<ClassEnrollment> findByStudentId(Long studentId, Pageable pageable) {
        return repository.findByStudentId(studentId, pageable);
    }
    @Override public Page<ClassEnrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status, Pageable pageable) {
        return repository.findByStudentIdAndStatus(studentId, status, pageable);
    }
    @Override public List<ClassEnrollment> findByClassEntityIdAndStatus(Long classId, EnrollmentStatus status) {
        return repository.findByClassEntityIdAndStatus(classId, status);
    }
    @Override public List<Long> findStudentIdsByStatus(EnrollmentStatus status) {
        return repository.findStudentIdsByStatus(status);
    }
    @Override public long countByClassEntityIdAndStatus(Long classId, EnrollmentStatus status) {
        return repository.countByClassEntityIdAndStatus(classId, status);
    }
    @Override public boolean existsByClassEntityIdAndStudentId(Long classId, Long studentId) {
        return repository.existsByClassEntityIdAndStudentId(classId, studentId);
    }
    @Override public boolean existsByClassEntityIdAndStudentIdAndStatus(Long classId, Long studentId, EnrollmentStatus status) {
        return repository.existsByClassEntityIdAndStudentIdAndStatus(classId, studentId, status);
    }
    @Override public boolean existsByStudentIdAndStatus(Long studentId, EnrollmentStatus status) {
        return repository.existsByStudentIdAndStatus(studentId, status);
    }
    @Override public List<ClassEnrollment> findAllByStudentIdAndStatus(Long studentId, EnrollmentStatus status) {
        return repository.findAllByStudentIdAndStatus(studentId, status);
    }
    @Override public Optional<ClassEnrollment> findByClassEntityIdAndStudentId(Long classId, Long studentId) {
        return repository.findByClassEntityIdAndStudentId(classId, studentId);
    }
    @Override public Optional<ClassEnrollment> findById(Long enrollmentId) { return repository.findById(enrollmentId); }
    @Override public ClassEnrollment save(ClassEnrollment enrollment) { return repository.save(enrollment); }
    @Override public List<ClassEnrollment> saveAll(Iterable<ClassEnrollment> enrollments) {
        return repository.saveAll(enrollments);
    }
    @Override public void delete(ClassEnrollment enrollment) { repository.delete(enrollment); }
}
