package com.lumie.attendance.application.service;

import com.lumie.attendance.application.dto.request.AttendanceSessionRequest;
import com.lumie.attendance.application.dto.response.AttendanceSessionResponse;
import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.entity.StudentReadModel;
import com.lumie.attendance.domain.exception.AttendanceErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.attendance.domain.repository.AttendanceRecordRepository;
import com.lumie.attendance.domain.repository.AttendanceSessionRepository;
import com.lumie.attendance.domain.repository.StudentReadRepository;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.classroom.api.ClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceSessionCommandService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_LATE_THRESHOLD_MINUTES = 10;

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final StudentReadRepository studentReadRepository;
    private final ClassService classService;

    public AttendanceSessionResponse createSession(AttendanceSessionRequest request) {
        String className = null;
        if (request.classId() != null) {
            String tenantSlug = TenantContextHolder.getRequiredTenant();
            className = classService.getClass(tenantSlug, request.classId())
                .map(ClassService.ClassData::name)
                .orElse(null);
        }

        AttendanceSession session = AttendanceSession.create(
            request.name(),
            request.sessionDate(),
            request.classId(),
            className,
            request.lateThresholdMinutes()
        );

        AttendanceSession saved = sessionRepository.save(session);

        List<Long> studentIds;
        if (request.classId() != null) {
            String tenantSlug = TenantContextHolder.getRequiredTenant();
            studentIds = classService.getEnrolledStudentIds(tenantSlug, request.classId());
        } else {
            studentIds = studentReadRepository.findByIsActiveTrue()
                .stream()
                .map(StudentReadModel::getId)
                .toList();
        }

        List<AttendanceRecord> records = createRecords(saved, studentIds);

        recordRepository.saveAll(records);

        log.info("Attendance session created: {} with {} students", saved.getId(), records.size());
        return AttendanceSessionResponse.fromWithoutRecords(saved);
    }

    /**
     * Gets today's attendance session for the class, creating it on demand if absent.
     * Idempotent on (classId, today): a second call returns the existing session rather
     * than creating a duplicate. Driven by a teacher opening the class to take attendance,
     * replacing the former daily pre-creation scheduler. "Today" is resolved in KST.
     */
    public AttendanceSessionResponse ensureSessionForClass(Long classId) {
        LocalDate today = LocalDate.now(KST);

        Optional<AttendanceSession> existing = sessionRepository.findByClassIdAndSessionDate(classId, today);
        if (existing.isPresent()) {
            log.debug("Returning existing attendance session {} for class {} on {}",
                existing.get().getId(), classId, today);
            return AttendanceSessionResponse.from(existing.get());
        }
        return createSessionForClass(classId, today);
    }

    private AttendanceSessionResponse createSessionForClass(Long classId, LocalDate date) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        String className = classService.getClass(tenantSlug, classId)
            .map(ClassService.ClassData::name)
            .orElseThrow(() -> new BusinessException(AttendanceErrorCode.CLASS_NOT_FOUND,
                "Class not found with id: %d".formatted(classId)));

        AttendanceSession session = AttendanceSession.create(
            className, date, classId, className, DEFAULT_LATE_THRESHOLD_MINUTES
        );
        AttendanceSession saved = sessionRepository.save(session);

        List<Long> studentIds = classService.getEnrolledStudentIds(tenantSlug, classId);
        List<AttendanceRecord> records = createRecords(saved, studentIds);
        recordRepository.saveAll(records);

        log.info("Created attendance session for class {} on {} with {} students",
            classId, date, records.size());
        return AttendanceSessionResponse.fromWithoutRecords(saved);
    }

    private static List<AttendanceRecord> createRecords(AttendanceSession session, List<Long> studentIds) {
        return studentIds.stream()
            .filter(Objects::nonNull)
            .map(studentId -> AttendanceRecord.create(session, studentId))
            .toList();
    }

    public AttendanceSessionResponse regenerateCode(Long id) {
        AttendanceSession session = sessionRepository.findById(id)
            .orElseThrow(() -> new BusinessException(AttendanceErrorCode.SESSION_NOT_FOUND,
                "Attendance session not found with id: %d".formatted(id)));

        session.regenerateCode();
        AttendanceSession saved = sessionRepository.save(session);

        log.info("Attendance code regenerated for session: {}", id);
        return AttendanceSessionResponse.fromWithoutRecords(saved);
    }

    public void deleteSession(Long id) {
        if (sessionRepository.findById(id).isEmpty()) {
            throw new BusinessException(AttendanceErrorCode.SESSION_NOT_FOUND,
                "Attendance session not found with id: %d".formatted(id));
        }

        sessionRepository.deleteById(id);

        log.info("Attendance session deleted: {}", id);
    }
}
