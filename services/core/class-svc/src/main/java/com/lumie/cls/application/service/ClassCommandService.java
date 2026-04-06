package com.lumie.cls.application.service;

import com.lumie.cls.application.dto.ClassEnrollmentResponse;
import com.lumie.cls.application.dto.ClassResponse;
import com.lumie.cls.application.dto.CreateClassRequest;
import com.lumie.cls.application.dto.EnrollStudentsRequest;
import com.lumie.cls.application.dto.UpdateClassRequest;
import com.lumie.cls.application.dto.UpdateEnrollmentRequest;
import com.lumie.cls.application.port.out.AcademyServicePort;
import com.lumie.cls.application.port.out.AdminServicePort;
import com.lumie.cls.domain.entity.ClassEnrollment;
import com.lumie.cls.domain.entity.ClassEntity;
import com.lumie.cls.domain.exception.ClassErrorCode;
import com.lumie.cls.domain.exception.ClassFullException;
import com.lumie.cls.domain.exception.ClassNotFoundException;
import com.lumie.cls.domain.exception.DuplicateEnrollmentException;
import com.lumie.cls.domain.exception.EnrollmentNotFoundException;
import com.lumie.cls.domain.repository.ClassEnrollmentRepository;
import com.lumie.cls.domain.repository.ClassRepository;
import com.lumie.cls.domain.vo.ClassStatus;
import com.lumie.cls.domain.vo.EnrollmentStatus;
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

    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final AcademyServicePort academyServicePort;
    private final AdminServicePort adminServicePort;

    public ClassResponse createClass(CreateClassRequest request) {
        academyServicePort.getAcademy(request.academyId())
            .filter(AcademyServicePort.AcademyInfo::isActive)
            .orElseThrow(() -> new BusinessException(ClassErrorCode.ACADEMY_NOT_FOUND,
                "Academy not found or inactive: " + request.academyId()));

        if (request.teacherId() != null) {
            adminServicePort.getAdmin(request.teacherId())
                .orElseThrow(() -> new BusinessException(ClassErrorCode.TEACHER_NOT_FOUND,
                    "Teacher not found with id: " + request.teacherId()));
        }

        if (classRepository.existsByNameAndAcademyId(request.name(), request.academyId())) {
            throw new BusinessException(ClassErrorCode.DUPLICATE_CLASS_NAME,
                "Class with name '" + request.name() + "' already exists in academy " + request.academyId());
        }

        ClassEntity entity = ClassEntity.create(
            request.academyId(),
            request.name(),
            request.subject(),
            request.teacherId(),
            request.maxStudents() != null ? request.maxStudents() : 0,
            request.tuitionAmount() != null ? request.tuitionAmount() : 0L,
            request.schedulePattern(),
            request.startDate(),
            request.endDate(),
            request.description()
        );

        ClassEntity saved = classRepository.save(entity);

        log.info("Class created: {} in academy: {}", saved.getId(), saved.getAcademyId());
        return ClassResponse.from(saved, 0L);
    }

    public ClassResponse updateClass(Long id, UpdateClassRequest request) {
        ClassEntity entity = classRepository.findById(id)
            .orElseThrow(() -> new ClassNotFoundException(id));

        String name = request.name() != null ? request.name() : entity.getName();
        String subject = request.subject() != null ? request.subject() : entity.getSubject();
        Long teacherId = request.teacherId() != null ? request.teacherId() : entity.getTeacherId();
        int maxStudents = request.maxStudents() != null ? request.maxStudents() : entity.getMaxStudents();
        long tuitionAmount = request.tuitionAmount() != null ? request.tuitionAmount() : entity.getTuitionAmount();
        var schedulePattern = request.schedulePattern() != null ? request.schedulePattern() : entity.getSchedulePattern();
        var startDate = request.startDate() != null ? request.startDate() : entity.getStartDate();
        var endDate = request.endDate() != null ? request.endDate() : entity.getEndDate();
        String description = request.description() != null ? request.description() : entity.getDescription();

        entity.updateInfo(name, subject, teacherId, maxStudents, tuitionAmount,
            schedulePattern, startDate, endDate, description);

        if (request.status() != null) {
            ClassStatus newStatus = parseClassStatus(request.status());
            switch (newStatus) {
                case ACTIVE -> entity.activate();
                case CLOSED -> entity.close();
                case PENDING -> {} // no-op, keep current state
            }
        }

        ClassEntity saved = classRepository.save(entity);

        long enrolledCount = enrollmentRepository.countByClassEntityIdAndStatus(saved.getId(), EnrollmentStatus.ENROLLED);
        log.info("Class updated: {}", saved.getId());
        return ClassResponse.from(saved, enrolledCount);
    }

    public void deleteClass(Long id) {
        ClassEntity entity = classRepository.findById(id)
            .orElseThrow(() -> new ClassNotFoundException(id));

        entity.close();
        classRepository.save(entity);

        log.info("Class closed (soft-deleted): {}", id);
    }

    public List<ClassEnrollmentResponse> enrollStudents(Long classId, EnrollStudentsRequest request) {
        ClassEntity classEntity = classRepository.findByIdWithLock(classId)
            .orElseThrow(() -> new ClassNotFoundException(classId));

        if (!classEntity.isActive()) {
            throw new BusinessException(ClassErrorCode.CLASS_CLOSED,
                "Cannot enroll students in a non-active class: " + classId);
        }

        long currentEnrolled = enrollmentRepository.countByClassEntityIdAndStatus(classId, EnrollmentStatus.ENROLLED);
        int newCount = request.studentIds().size();

        if (classEntity.getMaxStudents() > 0
                && currentEnrolled + newCount > classEntity.getMaxStudents()) {
            throw new ClassFullException(classId);
        }

        for (Long studentId : request.studentIds()) {
            if (enrollmentRepository.existsByClassEntityIdAndStudentId(classId, studentId)) {
                throw new DuplicateEnrollmentException(classId, studentId);
            }
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
            .orElseThrow(() -> new EnrollmentNotFoundException(enrollmentId));

        if (!enrollment.getClassEntity().getId().equals(classId)) {
            throw new EnrollmentNotFoundException(enrollmentId);
        }

        enrollment.drop();
        enrollmentRepository.save(enrollment);

        log.info("Enrollment {} dropped from class: {}", enrollmentId, classId);
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

    private ClassStatus parseClassStatus(String status) {
        try {
            return ClassStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ClassErrorCode.INVALID_STATUS, "Invalid class status: " + status);
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
