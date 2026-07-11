package com.lumie.classroom.application.service;

import com.lumie.classroom.application.dto.ClassEnrollmentResponse;
import com.lumie.classroom.application.dto.ClassResponse;
import com.lumie.classroom.domain.entity.ClassEnrollment;
import com.lumie.classroom.domain.entity.ClassEntity;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.staff.api.StaffService;
import com.lumie.classroom.domain.exception.ClassErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.classroom.domain.repository.ClassEnrollmentRepository;
import com.lumie.classroom.domain.repository.ClassRepository;
import com.lumie.classroom.domain.vo.ClassSearchCriteria;
import com.lumie.classroom.domain.vo.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    private static final String DELETED_TEACHER_NAME = "삭제된 강사";

    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final StaffService staffService;

    public ClassResponse getClass(Long id) {
        ClassEntity entity = classRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ClassErrorCode.CLASS_NOT_FOUND,
                "Class not found with id: %d".formatted(id)));

        String teacherName = resolveTeacherName(entity);
        return ClassResponse.from(entity, teacherName);
    }

    public Page<ClassResponse> listClasses(Long teacherId, String search, Pageable pageable) {
        ClassSearchCriteria criteria = new ClassSearchCriteria(teacherId, search);

        Page<ClassEntity> page = classRepository.findAll(criteria, pageable);
        Map<Long, String> teacherNames = resolveTeacherNames(page.getContent());

        return page.map(entity ->
            ClassResponse.from(entity, resolveTeacherName(entity, teacherNames))
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
        Pageable enrollmentPageable = mapEnrollmentSort(pageable);
        return enrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ENROLLED, enrollmentPageable)
            .map(enrollment -> {
                ClassEntity entity = enrollment.getClassEntity();
                String teacherName = resolveTeacherName(entity);
                return ClassResponse.from(entity, teacherName);
            });
    }

    private Pageable mapEnrollmentSort(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return pageable;
        }
        Sort sort = Sort.by(pageable.getSort().stream().map(order -> {
            if ("name".equals(order.getProperty())) {
                return order.withProperty("classEntity.name");
            }
            return order;
        }).toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private String resolveTeacherName(ClassEntity entity) {
        return resolveTeacherName(entity, Map.of());
    }

    private String resolveTeacherName(ClassEntity entity, Map<Long, String> knownNames) {
        Long teacherId = entity.getTeacherId();
        if (teacherId == null) {
            return entity.isTeacherDeleted() ? DELETED_TEACHER_NAME : null;
        }
        if (knownNames.containsKey(teacherId)) {
            return knownNames.get(teacherId);
        }
        String slug = TenantContextHolder.getRequiredTenant();
        return staffService.getStaff(slug, teacherId)
            .map(StaffService.StaffData::name)
            .orElse(DELETED_TEACHER_NAME);
    }

    private String resolveTeacherName(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        String slug = TenantContextHolder.getRequiredTenant();
        return staffService.getStaff(slug, teacherId)
            .map(StaffService.StaffData::name)
            .orElse(DELETED_TEACHER_NAME);
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
            .map(ClassEnrollment::getStudentId)
            .filter(Objects::nonNull)
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

    public boolean isStudentEnrolledInClass(Long studentId, Long classId) {
        return enrollmentRepository.existsByClassEntityIdAndStudentIdAndStatus(
            classId,
            studentId,
            EnrollmentStatus.ENROLLED
        );
    }

    public boolean hasActiveEnrollment(Long studentId) {
        return enrollmentRepository.existsByStudentIdAndStatus(studentId, EnrollmentStatus.ENROLLED);
    }

    public boolean hasAssignedClasses(Long staffId) {
        return classRepository.existsByTeacherId(staffId);
    }

    public record StudentClassRow(Long classId, String className, Long studentId) {}

    public Page<ClassEnrollmentResponse> getEnrollments(Long classId, Pageable pageable) {
        return enrollmentRepository.findByClassEntityId(classId, pageable)
            .map(ClassEnrollmentResponse::from);
    }
}
