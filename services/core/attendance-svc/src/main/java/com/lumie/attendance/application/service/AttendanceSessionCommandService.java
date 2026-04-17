package com.lumie.attendance.application.service;

import com.lumie.attendance.application.dto.AttendanceSessionRequest;
import com.lumie.attendance.application.dto.AttendanceSessionResponse;
import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.entity.StudentReadModel;
import com.lumie.attendance.domain.exception.SessionNotFoundException;
import com.lumie.attendance.domain.repository.AttendanceRecordRepository;
import com.lumie.attendance.domain.repository.AttendanceSessionRepository;
import com.lumie.attendance.domain.repository.StudentReadRepository;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.internal.cls.ClassInternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceSessionCommandService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final StudentReadRepository studentReadRepository;
    private final ClassInternalService classInternalService;

    public AttendanceSessionResponse createSession(AttendanceSessionRequest request) {
        String className = null;
        if (request.classId() != null) {
            String tenantSlug = TenantContextHolder.getRequiredTenant();
            className = classInternalService.getClass(tenantSlug, request.classId())
                .map(ClassInternalService.ClassData::name)
                .orElse(null);
        }

        AttendanceSession session = AttendanceSession.create(
            request.academyId(),
            request.name(),
            request.sessionDate(),
            request.classId(),
            className,
            request.lateThresholdMinutes(),
            null
        );

        AttendanceSession saved = sessionRepository.save(session);

        List<Long> studentIds;
        if (request.classId() != null) {
            String tenantSlug = TenantContextHolder.getRequiredTenant();
            studentIds = classInternalService.getEnrolledStudentIds(tenantSlug, request.classId());
        } else {
            studentIds = studentReadRepository.findByAcademyIdAndIsActiveTrue(request.academyId())
                .stream()
                .map(StudentReadModel::getId)
                .toList();
        }

        List<AttendanceRecord> records = studentIds.stream()
            .map(studentId -> AttendanceRecord.create(saved, studentId))
            .toList();

        recordRepository.saveAll(records);

        log.info("Attendance session created: {} with {} students", saved.getId(), records.size());
        return AttendanceSessionResponse.fromWithoutRecords(saved);
    }

    public void createSessionForClass(Long academyId, Long classId, String className,
                                      LocalDate date, Integer lateThresholdMinutes) {
        if (sessionRepository.existsByClassIdAndSessionDate(classId, date)) {
            log.debug("Session already exists for class {} on {}, skipping", classId, date);
            return;
        }

        AttendanceSession session = AttendanceSession.create(
            academyId, className, date, classId, className,
            lateThresholdMinutes, null
        );

        AttendanceSession saved = sessionRepository.save(session);

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        List<Long> studentIds = classInternalService.getEnrolledStudentIds(tenantSlug, classId);

        List<AttendanceRecord> records = studentIds.stream()
            .map(studentId -> AttendanceRecord.create(saved, studentId))
            .toList();

        recordRepository.saveAll(records);

        log.info("Auto-created session for class {} on {} with {} students", classId, date, records.size());
    }

    public AttendanceSessionResponse regenerateCode(Long id) {
        AttendanceSession session = sessionRepository.findById(id)
            .orElseThrow(() -> new SessionNotFoundException(id));

        session.regenerateCode();
        AttendanceSession saved = sessionRepository.save(session);

        log.info("Attendance code regenerated for session: {}", id);
        return AttendanceSessionResponse.fromWithoutRecords(saved);
    }

    public void deleteSession(Long id) {
        sessionRepository.findById(id)
            .orElseThrow(() -> new SessionNotFoundException(id));

        sessionRepository.deleteById(id);

        log.info("Attendance session deleted: {}", id);
    }
}
