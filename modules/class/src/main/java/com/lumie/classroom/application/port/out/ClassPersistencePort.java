package com.lumie.classroom.application.port.out;

import com.lumie.classroom.domain.entity.ClassEntity;
import com.lumie.classroom.domain.vo.ClassSearchCriteria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.lumie.classroom.domain.vo.EnrollmentStatus;

public interface ClassPersistencePort {
    Optional<ClassEntity> findByIdWithLock(Long id);
    Page<ClassEntity> findByTeacherId(Long teacherId, Pageable pageable);
    Page<ClassEntity> findEnrolledClassesByStudentIdAndStatus(Long studentId, EnrollmentStatus status, Pageable pageable);
    List<ClassEntity> findAllByTeacherId(Long teacherId);
    boolean existsByTeacherId(Long teacherId);
    Page<ClassEntity> findAll(ClassSearchCriteria criteria, Pageable pageable);
    Optional<ClassEntity> findById(Long id);
    ClassEntity save(ClassEntity entity);
    void delete(ClassEntity entity);
    List<ClassEntity> findAllById(Iterable<Long> ids);
    long count();
}
