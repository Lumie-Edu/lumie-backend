package com.lumie.attendance.application.service;

import com.lumie.attendance.application.dto.AttendanceSessionRequest;
import com.lumie.attendance.application.dto.AttendanceSessionResponse;
import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.entity.StudentReadModel;
import com.lumie.attendance.domain.exception.SessionClosedException;
import com.lumie.attendance.domain.exception.SessionNotFoundException;
import com.lumie.attendance.domain.repository.AttendanceRecordRepository;
import com.lumie.attendance.domain.repository.AttendanceSessionRepository;
import com.lumie.attendance.domain.repository.StudentReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceSessionCommandService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final StudentReadRepository studentReadRepository;

    public AttendanceSessionResponse createSession(AttendanceSessionRequest request) {
        AttendanceSession session = AttendanceSession.create(
            request.academyId(),
            request.name(),
            request.sessionDate(),
            request.subject(),
            request.lateThresholdMinutes(),
            null
        );

        AttendanceSession saved = sessionRepository.save(session);

        List<StudentReadModel> activeStudents =
            studentReadRepository.findByAcademyIdAndIsActiveTrue(request.academyId());

        List<AttendanceRecord> records = activeStudents.stream()
            .map(student -> AttendanceRecord.create(saved, student.getId()))
            .toList();

        recordRepository.saveAll(records);

        log.info("Attendance session created: {} with {} students", saved.getId(), records.size());
        return AttendanceSessionResponse.fromWithoutRecords(saved);
    }

    public void closeSession(Long id) {
        AttendanceSession session = sessionRepository.findById(id)
            .orElseThrow(() -> new SessionNotFoundException(id));

        session.close();
        sessionRepository.save(session);

        log.info("Attendance session closed: {}", id);
    }

    public AttendanceSessionResponse regenerateCode(Long id) {
        AttendanceSession session = sessionRepository.findById(id)
            .orElseThrow(() -> new SessionNotFoundException(id));

        if (!session.isOpen()) {
            throw new SessionClosedException(id);
        }

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
