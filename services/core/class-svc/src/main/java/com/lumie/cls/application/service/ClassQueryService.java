package com.lumie.cls.application.service;

import com.lumie.cls.application.dto.ClassEnrollmentResponse;
import com.lumie.cls.application.dto.ClassResponse;
import com.lumie.cls.domain.entity.ClassEntity;
import com.lumie.cls.domain.exception.ClassNotFoundException;
import com.lumie.cls.domain.repository.ClassEnrollmentRepository;
import com.lumie.cls.domain.repository.ClassRepository;
import com.lumie.cls.domain.vo.ClassSearchCriteria;
import com.lumie.cls.domain.vo.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassQueryService {

    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository enrollmentRepository;

    public ClassResponse getClass(Long id) {
        ClassEntity entity = classRepository.findById(id)
            .orElseThrow(() -> new ClassNotFoundException(id));

        long enrolledCount = enrollmentRepository.countByClassEntityIdAndStatus(id, EnrollmentStatus.ENROLLED);
        return ClassResponse.from(entity, enrolledCount);
    }

    public Page<ClassResponse> listClasses(Long academyId, Long teacherId,
                                           String search, Pageable pageable) {
        ClassSearchCriteria criteria = new ClassSearchCriteria(academyId, teacherId, search);

        return classRepository.findAll(criteria, pageable)
            .map(entity -> {
                long enrolledCount = enrollmentRepository.countByClassEntityIdAndStatus(
                    entity.getId(), EnrollmentStatus.ENROLLED);
                return ClassResponse.from(entity, enrolledCount);
            });
    }

    public Page<ClassResponse> getClassesByTeacher(Long teacherId, Pageable pageable) {
        return classRepository.findByTeacherId(teacherId, pageable)
            .map(entity -> {
                long enrolledCount = enrollmentRepository.countByClassEntityIdAndStatus(
                    entity.getId(), EnrollmentStatus.ENROLLED);
                return ClassResponse.from(entity, enrolledCount);
            });
    }

    public Page<ClassResponse> getClassesByStudent(Long studentId, Pageable pageable) {
        return enrollmentRepository.findByStudentId(studentId, pageable)
            .map(enrollment -> {
                ClassEntity entity = enrollment.getClassEntity();
                long enrolledCount = enrollmentRepository.countByClassEntityIdAndStatus(
                    entity.getId(), EnrollmentStatus.ENROLLED);
                return ClassResponse.from(entity, enrolledCount);
            });
    }

    public Page<ClassEnrollmentResponse> getEnrollments(Long classId, Pageable pageable) {
        return enrollmentRepository.findByClassEntityId(classId, pageable)
            .map(ClassEnrollmentResponse::from);
    }
}
