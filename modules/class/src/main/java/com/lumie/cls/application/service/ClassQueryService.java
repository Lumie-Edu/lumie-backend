package com.lumie.cls.application.service;

import com.lumie.cls.application.dto.ClassEnrollmentResponse;
import com.lumie.cls.application.dto.ClassResponse;
import com.lumie.cls.application.port.out.AdminServicePort;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassQueryService {

    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final AdminServicePort adminServicePort;

    public ClassResponse getClass(Long id) {
        ClassEntity entity = classRepository.findById(id)
            .orElseThrow(() -> new ClassNotFoundException(id));

        String teacherName = resolveTeacherName(entity.getTeacherId());
        return ClassResponse.from(entity, teacherName);
    }

    public Page<ClassResponse> listClasses(Long teacherId, String search, Pageable pageable) {
        ClassSearchCriteria criteria = new ClassSearchCriteria(teacherId, search);

        Page<ClassEntity> page = classRepository.findAll(criteria, pageable);
        Map<Long, String> teacherNames = resolveTeacherNames(page.getContent());

        return page.map(entity ->
            ClassResponse.from(entity, teacherNames.get(entity.getTeacherId()))
        );
    }

    public Page<ClassResponse> getClassesByTeacher(Long teacherId, Pageable pageable) {
        Page<ClassEntity> page = classRepository.findByTeacherId(teacherId, pageable);
        String teacherName = resolveTeacherName(teacherId);

        return page.map(entity ->
            ClassResponse.from(entity, teacherName)
        );
    }

    public Page<ClassResponse> getClassesByStudent(Long studentId, Pageable pageable) {
        return enrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ENROLLED, pageable)
            .map(enrollment -> {
                ClassEntity entity = enrollment.getClassEntity();
                String teacherName = resolveTeacherName(entity.getTeacherId());
                return ClassResponse.from(entity, teacherName);
            });
    }

    private String resolveTeacherName(Long teacherId) {
        if (teacherId == null) return null;
        return adminServicePort.getAdmin(teacherId)
            .map(AdminServicePort.AdminInfo::name)
            .orElse(null);
    }

    private Map<Long, String> resolveTeacherNames(List<ClassEntity> classes) {
        Map<Long, String> names = new HashMap<>();
        classes.stream()
            .map(ClassEntity::getTeacherId)
            .filter(Objects::nonNull)
            .distinct()
            .forEach(id -> names.put(id, resolveTeacherName(id)));
        return names;
    }

    public List<Long> getEnrolledStudentIds(Long classId) {
        return enrollmentRepository.findByClassEntityIdAndStatus(classId, EnrollmentStatus.ENROLLED)
            .stream()
            .map(enrollment -> enrollment.getStudentId())
            .toList();
    }

    public List<StudentClassRow> getEnrolledClassesForStudents(Collection<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        return enrollmentRepository.findByStudentIdInAndStatus(studentIds, EnrollmentStatus.ENROLLED)
            .stream()
            .map(enrollment -> {
                ClassEntity entity = enrollment.getClassEntity();
                return new StudentClassRow(entity.getId(), entity.getName(), enrollment.getStudentId());
            })
            .toList();
    }

    public record StudentClassRow(Long classId, String className, Long studentId) {}

    public Page<ClassEnrollmentResponse> getEnrollments(Long classId, Pageable pageable) {
        return enrollmentRepository.findByClassEntityId(classId, pageable)
            .map(ClassEnrollmentResponse::from);
    }
}
