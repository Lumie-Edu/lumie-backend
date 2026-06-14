package com.lumie.classroom.application.service;

import com.lumie.classroom.application.dto.ClassEnrollmentResponse;
import com.lumie.classroom.application.dto.ClassResponse;
import com.lumie.classroom.application.dto.CreateClassRequest;
import com.lumie.classroom.application.dto.EnrollStudentsRequest;
import com.lumie.classroom.application.dto.UpdateClassRequest;
import com.lumie.classroom.application.dto.UpdateEnrollmentRequest;
import com.lumie.classroom.domain.entity.ClassEnrollment;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.staff.api.StaffService;
import com.lumie.classroom.domain.entity.ClassEntity;
import com.lumie.classroom.domain.exception.ClassErrorCode;
import com.lumie.classroom.domain.repository.ClassEnrollmentRepository;
import com.lumie.classroom.domain.repository.ClassRepository;
import com.lumie.classroom.domain.vo.EnrollmentStatus;
import com.lumie.classroom.domain.vo.SchedulePattern;
import com.lumie.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ClassCommandService {

    private static final String DELETED_TEACHER_NAME = "삭제된 강사";

    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final StaffService staffService;

    public ClassResponse createClass(CreateClassRequest request) {
        if (request.teacherId() != null) {
            staffService.getStaff(TenantContextHolder.getRequiredTenant(), request.teacherId())
                .orElseThrow(() -> new BusinessException(ClassErrorCode.TEACHER_NOT_FOUND,
                    "Teacher not found with id: " + request.teacherId()));
        }

        SchedulePattern createPattern = request.schedulePattern() != null ? request.schedulePattern().toVo() : null;
        validateNoScheduleOverlap(request.teacherId(), createPattern, null);

        ClassEntity entity = ClassEntity.create(
            request.name(),
            request.teacherId(),
            createPattern,
            request.description()
        );

        ClassEntity saved = classRepository.save(entity);

        log.info("Class created: {}", saved.getId());
        String teacherName = resolveTeacherName(saved);
        return ClassResponse.from(saved, teacherName);
    }

    public ClassResponse updateClass(Long id, UpdateClassRequest request) {
        ClassEntity entity = classRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ClassErrorCode.CLASS_NOT_FOUND,
                "Class not found with id: %d".formatted(id)));

        String name = request.name() != null ? request.name() : entity.getName();
        Long teacherId = request.teacherId() != null ? request.teacherId() : entity.getTeacherId();
        SchedulePattern schedulePattern = request.schedulePattern() != null ? request.schedulePattern().toVo() : entity.getSchedulePattern();
        String description = request.description() != null ? request.description() : entity.getDescription();

        validateNoScheduleOverlap(teacherId, schedulePattern, id);

        entity.updateInfo(name, teacherId, schedulePattern, description);

        ClassEntity saved = classRepository.save(entity);

        log.info("Class updated: {}", saved.getId());
        String updatedTeacherName = resolveTeacherName(saved);
        return ClassResponse.from(saved, updatedTeacherName);
    }

    private String resolveTeacherName(ClassEntity entity) {
        if (entity.getTeacherId() == null) {
            return entity.isTeacherDeleted() ? DELETED_TEACHER_NAME : null;
        }
        return staffService.getStaff(TenantContextHolder.getRequiredTenant(), entity.getTeacherId())
            .map(StaffService.StaffData::name)
            .orElse(DELETED_TEACHER_NAME);
    }

    public void deleteClass(Long id) {
        ClassEntity entity = classRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ClassErrorCode.CLASS_NOT_FOUND,
                "Class not found with id: %d".formatted(id)));

        classRepository.delete(entity);

        log.info("Class deleted: {}", id);
    }

    public List<ClassEnrollmentResponse> enrollStudents(Long classId, EnrollStudentsRequest request) {
        ClassEntity classEntity = classRepository.findByIdWithLock(classId)
            .orElseThrow(() -> new BusinessException(ClassErrorCode.CLASS_NOT_FOUND,
                "Class not found with id: %d".formatted(classId)));

        for (Long studentId : request.studentIds()) {
            if (enrollmentRepository.existsByClassEntityIdAndStudentId(classId, studentId)) {
                throw new BusinessException(ClassErrorCode.DUPLICATE_ENROLLMENT,
                    "Student %d is already enrolled in class %d".formatted(studentId, classId));
            }
            validateNoStudentScheduleOverlap(studentId, classEntity);
        }

        List<ClassEnrollment> enrollments = request.studentIds().stream()
            .map(studentId -> ClassEnrollment.create(classEntity, studentId))
            .toList();

        List<ClassEnrollment> saved = enrollmentRepository.saveAll(enrollments);

        log.info("Enrolled {} students in class: {}", saved.size(), classId);
        return saved.stream()
            .map(ClassEnrollmentResponse::from)
            .toList();
    }

    public void removeEnrollment(Long classId, Long enrollmentId) {
        ClassEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new BusinessException(ClassErrorCode.ENROLLMENT_NOT_FOUND,
                "Enrollment not found with id: %d".formatted(enrollmentId)));

        if (!enrollment.getClassEntity().getId().equals(classId)) {
            throw new BusinessException(ClassErrorCode.ENROLLMENT_NOT_FOUND,
                "Enrollment not found with id: %d".formatted(enrollmentId));
        }

        enrollmentRepository.delete(enrollment);

        log.info("Enrollment {} removed from class: {}", enrollmentId, classId);
    }

    public void removeEnrollmentByStudent(Long classId, Long studentId) {
        ClassEnrollment enrollment = enrollmentRepository.findByClassEntityIdAndStudentId(classId, studentId)
            .orElseThrow(() -> new BusinessException(ClassErrorCode.ENROLLMENT_NOT_FOUND,
                "Enrollment not found for class %d and student %d".formatted(classId, studentId)));

        enrollmentRepository.delete(enrollment);

        log.info("Student {} removed from class: {}", studentId, classId);
    }

    public ClassEnrollmentResponse updateEnrollmentStatus(Long classId, Long enrollmentId,
                                                          UpdateEnrollmentRequest request) {
        ClassEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new BusinessException(ClassErrorCode.ENROLLMENT_NOT_FOUND,
                "Enrollment not found with id: %d".formatted(enrollmentId)));

        if (!enrollment.getClassEntity().getId().equals(classId)) {
            throw new BusinessException(ClassErrorCode.ENROLLMENT_NOT_FOUND,
                "Enrollment not found with id: %d".formatted(enrollmentId));
        }

        EnrollmentStatus newStatus = parseEnrollmentStatus(request.status());
        switch (newStatus) {
            case DROPPED -> enrollment.drop();
            case COMPLETED -> enrollment.complete();
            case ENROLLED -> {} // no-op
        }

        ClassEnrollment saved = enrollmentRepository.save(enrollment);

        log.info("Enrollment {} status updated to {} in class: {}", enrollmentId, request.status(), classId);
        return ClassEnrollmentResponse.from(saved);
    }

    private void validateNoScheduleOverlap(Long teacherId, SchedulePattern newPattern, Long excludeClassId) {
        if (teacherId == null || hasNoSessions(newPattern)) {
            return;
        }

        List<ClassEntity> teacherClasses = classRepository.findAllByTeacherId(teacherId);

        for (ClassEntity existing : teacherClasses) {
            if (excludeClassId != null && excludeClassId.equals(existing.getId())) {
                continue;
            }
            SchedulePattern ep = existing.getSchedulePattern();
            if (hasNoSessions(ep)) {
                continue;
            }
            if (hasTimeOverlap(newPattern, ep)) {
                throw new BusinessException(ClassErrorCode.SCHEDULE_OVERLAP,
                        "Teacher " + teacherId + " already has class '" + existing.getName()
                                + "' at overlapping time");
            }
        }
    }

    private void validateNoStudentScheduleOverlap(Long studentId, ClassEntity targetClass) {
        SchedulePattern newPattern = targetClass.getSchedulePattern();
        if (hasNoSessions(newPattern)) {
            return;
        }

        List<ClassEnrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        for (ClassEnrollment enrollment : enrollments) {
            if (enrollment.getStatus() != EnrollmentStatus.ENROLLED) {
                continue;
            }
            ClassEntity existing = enrollment.getClassEntity();
            if (existing.getId().equals(targetClass.getId())) {
                continue;
            }
            SchedulePattern ep = existing.getSchedulePattern();
            if (hasNoSessions(ep)) {
                continue;
            }
            if (hasTimeOverlap(newPattern, ep)) {
                throw new BusinessException(ClassErrorCode.STUDENT_SCHEDULE_OVERLAP,
                        "Student " + studentId + " already has class '" + existing.getName()
                                + "' at overlapping time");
            }
        }
    }

    private boolean hasTimeOverlap(SchedulePattern a, SchedulePattern b) {
        for (SchedulePattern.ScheduleSession left : a.sessions()) {
            for (SchedulePattern.ScheduleSession right : b.sessions()) {
                if (left.day().equals(right.day()) && hasTimeOverlap(left, right)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasTimeOverlap(SchedulePattern.ScheduleSession a, SchedulePattern.ScheduleSession b) {
        return a.startTime().compareTo(b.endTime()) < 0
                && a.endTime().compareTo(b.startTime()) > 0;
    }

    private boolean hasNoSessions(SchedulePattern pattern) {
        return pattern == null || pattern.sessions().isEmpty();
    }

    private EnrollmentStatus parseEnrollmentStatus(String status) {
        try {
            return EnrollmentStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ClassErrorCode.INVALID_STATUS, "Invalid enrollment status: " + status);
        }
    }
}
