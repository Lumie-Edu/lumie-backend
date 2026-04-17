package com.lumie.cls.adapter.out.persistence;

import com.lumie.cls.domain.entity.ClassEnrollment;
import com.lumie.cls.domain.repository.ClassEnrollmentRepository;
import com.lumie.cls.domain.vo.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ClassEnrollmentRepositoryAdapter implements ClassEnrollmentRepository {

    private final JpaClassEnrollmentRepository jpaRepository;

    @Override
    public ClassEnrollment save(ClassEnrollment enrollment) {
        return jpaRepository.save(enrollment);
    }

    @Override
    public List<ClassEnrollment> saveAll(List<ClassEnrollment> enrollments) {
        return jpaRepository.saveAll(enrollments);
    }

    @Override
    public Optional<ClassEnrollment> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<ClassEnrollment> findByClassEntityId(Long classId, Pageable pageable) {
        return jpaRepository.findByClassEntityId(classId, pageable);
    }

    @Override
    public List<ClassEnrollment> findByStudentId(Long studentId) {
        return jpaRepository.findByStudentId(studentId);
    }

    @Override
    public List<ClassEnrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status) {
        return jpaRepository.findByStudentIdAndStatus(studentId, status);
    }

    @Override
    public Page<ClassEnrollment> findByStudentId(Long studentId, Pageable pageable) {
        return jpaRepository.findByStudentId(studentId, pageable);
    }

    @Override
    public Page<ClassEnrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status, Pageable pageable) {
        return jpaRepository.findByStudentIdAndStatus(studentId, status, pageable);
    }

    @Override
    public long countByClassEntityIdAndStatus(Long classId, EnrollmentStatus status) {
        return jpaRepository.countByClassEntityIdAndStatus(classId, status);
    }

    @Override
    public boolean existsByClassEntityIdAndStudentId(Long classId, Long studentId) {
        return jpaRepository.existsByClassEntityIdAndStudentId(classId, studentId);
    }

    @Override
    public Optional<ClassEnrollment> findByClassEntityIdAndStudentId(Long classId, Long studentId) {
        return jpaRepository.findByClassEntityIdAndStudentId(classId, studentId);
    }
}
