package com.lumie.cls.application.service;

import com.lumie.cls.application.dto.ClassEnrollmentResponse;
import com.lumie.cls.application.dto.ClassResponse;
import com.lumie.cls.application.dto.CreateClassRequest;
import com.lumie.cls.application.dto.EnrollStudentsRequest;
import com.lumie.cls.application.dto.UpdateClassRequest;
import com.lumie.cls.application.dto.UpdateEnrollmentRequest;
import com.lumie.cls.domain.entity.ClassEnrollment;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.admin.api.AdminService;
import com.lumie.cls.domain.entity.ClassEntity;
import com.lumie.cls.domain.exception.ClassErrorCode;
import com.lumie.cls.domain.exception.ClassNotFoundException;
import com.lumie.cls.domain.exception.DuplicateEnrollmentException;
import com.lumie.cls.domain.exception.EnrollmentNotFoundException;
import com.lumie.cls.domain.repository.ClassEnrollmentRepository;
import com.lumie.cls.domain.repository.ClassRepository;
import com.lumie.cls.domain.vo.EnrollmentStatus;
import com.lumie.cls.domain.vo.SchedulePattern;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.activitylog.api.ActivityLogService;
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

    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final AdminService adminService;
    private final ActivityLogService activityLogService;

    public ClassResponse createClass(CreateClassRequest request) {
        if (request.teacherId() != null) {
            adminService.getAdmin(TenantContextHolder.getRequiredTenant(), request.teacherId())
                .orElseThrow(() -> new BusinessException(ClassErrorCode.TEACHER_NOT_FOUND,
                    "Teacher not found with id: " + request.teacherId()));
        }

        validateNoScheduleOverlap(request.teacherId(), request.schedulePattern(), null);

        ClassEntity entity = ClassEntity.create(
            request.name(),
            request.teacherId(),
            request.schedulePattern(),
            request.description()
        );

        ClassEntity saved = classRepository.save(entity);

        log.info("Class created: {}", saved.getId());
        logActivity("CREATE", "CLASS", saved.getId(), saved.getName(), null);
        String teacherName = saved.getTeacherId() != null
            ? adminService.getAdmin(TenantContextHolder.getRequiredTenant(), saved.getTeacherId()).map(AdminService.AdminData::name).orElse(null)
            : null;
        return ClassResponse.from(saved, teacherName);
    }

    public ClassResponse updateClass(Long id, UpdateClassRequest request) {
        ClassEntity entity = classRepository.findById(id)
            .orElseThrow(() -> new ClassNotFoundException(id));

        String name = request.name() != null ? request.name() : entity.getName();
        Long teacherId = request.teacherId() != null ? request.teacherId() : entity.getTeacherId();
        var schedulePattern = request.schedulePattern() != null ? request.schedulePattern() : entity.getSchedulePattern();
        String description = request.description() != null ? request.description() : entity.getDescription();

        validateNoScheduleOverlap(teacherId, schedulePattern, id);

        entity.updateInfo(name, teacherId, schedulePattern, description);

        ClassEntity saved = classRepository.save(entity);

        log.info("Class updated: {}", saved.getId());
        logActivity("UPDATE", "CLASS", saved.getId(), saved.getName(), null);
        String updatedTeacherName = saved.getTeacherId() != null
            ? adminService.getAdmin(TenantContextHolder.getRequiredTenant(), saved.getTeacherId()).map(AdminService.AdminData::name).orElse(null)
            : null;
        return ClassResponse.from(saved, updatedTeacherName);
    }

    public void deleteClass(Long id) {
        ClassEntity entity = classRepository.findById(id)
            .orElseThrow(() -> new ClassNotFoundException(id));

        String className = entity.getName();
        classRepository.delete(entity);

        log.info("Class deleted: {}", id);
        logActivity("DELETE", "CLASS", id, className, null);
    }

    public List<ClassEnrollmentResponse> enrollStudents(Long classId, EnrollStudentsRequest request) {
        ClassEntity classEntity = classRepository.findByIdWithLock(classId)
            .orElseThrow(() -> new ClassNotFoundException(classId));

        for (Long studentId : request.studentIds()) {
            if (enrollmentRepository.existsByClassEntityIdAndStudentId(classId, studentId)) {
                throw new DuplicateEnrollmentException(classId, studentId);
            }
            validateNoStudentScheduleOverlap(studentId, classEntity);
        }

        List<ClassEnrollment> enrollments = request.studentIds().stream()
            .map(studentId -> ClassEnrollment.create(classEntity, studentId))
            .toList();

        List<ClassEnrollment> saved = enrollmentRepository.saveAll(enrollments);

        log.info("Enrolled {} students in class: {}", saved.size(), classId);
        logActivity("ENROLL", "CLASS", classId, classEntity.getName(),
                saved.size() + " students enrolled");
        return saved.stream()
            .map(ClassEnrollmentResponse::from)
            .toList();
    }

    public void removeEnrollment(Long classId, Long enrollmentId) {
        ClassEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        if (!enrollment.getClassEntity().getId().equals(classId)) {
            throw new EnrollmentNotFoundException(enrollmentId);
        }

        enrollmentRepository.delete(enrollment);

        log.info("Enrollment {} removed from class: {}", enrollmentId, classId);
        logActivity("DROP", "CLASS", classId, enrollment.getClassEntity().getName(), null);
    }

    public void removeEnrollmentByStudent(Long classId, Long studentId) {
        ClassEnrollment enrollment = enrollmentRepository.findByClassEntityIdAndStudentId(classId, studentId)
            .orElseThrow(() -> new EnrollmentNotFoundException(classId, studentId));

        enrollmentRepository.delete(enrollment);

        log.info("Student {} removed from class: {}", studentId, classId);
        logActivity("DROP", "CLASS", classId, enrollment.getClassEntity().getName(), null);
    }

    public ClassEnrollmentResponse updateEnrollmentStatus(Long classId, Long enrollmentId,
                                                          UpdateEnrollmentRequest request) {
        ClassEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        if (!enrollment.getClassEntity().getId().equals(classId)) {
            throw new EnrollmentNotFoundException(enrollmentId);
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
        if (teacherId == null || newPattern == null || newPattern.days().isEmpty()) {
            return;
        }

        List<ClassEntity> teacherClasses = classRepository.findAllByTeacherId(teacherId);

        for (ClassEntity existing : teacherClasses) {
            if (excludeClassId != null && excludeClassId.equals(existing.getId())) {
                continue;
            }
            SchedulePattern ep = existing.getSchedulePattern();
            if (ep == null || ep.days().isEmpty()) {
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
        if (newPattern == null || newPattern.days().isEmpty()) {
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
            if (ep == null || ep.days().isEmpty()) {
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
        boolean daysOverlap = a.days().stream().anyMatch(b.days()::contains);
        if (!daysOverlap) {
            return false;
        }
        return a.startTime().compareTo(b.endTime()) < 0
                && a.endTime().compareTo(b.startTime()) > 0;
    }

    private void logActivity(String action, String entityType, Long entityId,
                             String entityName, String description) {
        try {
            Long userId = UserContextHolder.getRequiredUserId();
            String role = UserContextHolder.getUserRole();
            adminService.getAdminByUserId(TenantContextHolder.getRequiredTenant(), userId).ifPresent(actor ->
                activityLogService.log(actor.id(), actor.name(), role,
                    action, entityType, entityId, entityName, description)
            );
        } catch (Exception e) {
            log.warn("Failed to log activity: {}", e.getMessage());
        }
    }

    private EnrollmentStatus parseEnrollmentStatus(String status) {
        try {
            return EnrollmentStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ClassErrorCode.INVALID_STATUS, "Invalid enrollment status: " + status);
        }
    }
}
