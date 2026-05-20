package com.lumie.attendance.application.service;

import com.lumie.attendance.application.dto.request.BulkUpdateStatusRequest;
import com.lumie.attendance.application.dto.request.CheckInRequest;
import com.lumie.attendance.application.dto.request.UpdateAttendanceStatusRequest;
import com.lumie.attendance.application.dto.response.CheckInResponse;
import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.entity.StudentReadModel;
import com.lumie.attendance.domain.exception.InvalidCodeException;
import com.lumie.attendance.domain.exception.RecordNotFoundException;
import com.lumie.attendance.domain.exception.SessionNotFoundException;
import com.lumie.attendance.domain.repository.AttendanceRecordRepository;
import com.lumie.attendance.domain.repository.AttendanceSessionRepository;
import com.lumie.attendance.domain.repository.StudentReadRepository;
import com.lumie.attendance.domain.vo.AttendanceStatus;
import com.lumie.common.exception.BusinessException;
import com.lumie.attendance.domain.exception.AttendanceErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceRecordCommandService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final StudentReadRepository studentReadRepository;

    public CheckInResponse checkInByCode(Long userId, CheckInRequest request) {
        StudentReadModel student = studentReadRepository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException(AttendanceErrorCode.STUDENT_NOT_FOUND,
                "Student not found for user: " + userId));

        AttendanceSession session = sessionRepository.findByAttendanceCode(request.code())
            .orElseThrow(InvalidCodeException::new);

        if (session.isExpired()) {
            throw new BusinessException(AttendanceErrorCode.CODE_EXPIRED);
        }

        AttendanceRecord record = recordRepository.findBySessionIdAndStudentId(
                session.getId(), student.getId())
            .orElseThrow(() -> new BusinessException(AttendanceErrorCode.RECORD_NOT_FOUND,
                "No attendance record found for this student in session"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sessionStart = session.getSessionDate().atStartOfDay();
        long minutesSinceStart = java.time.Duration.between(sessionStart, now).toMinutes();

        AttendanceStatus status = minutesSinceStart > session.getLateThresholdMinutes()
            ? AttendanceStatus.LATE
            : AttendanceStatus.PRESENT;

        record.checkIn(status, userId);
        recordRepository.save(record);

        String message = status == AttendanceStatus.PRESENT
            ? "출석이 완료되었습니다."
            : "지각으로 처리되었습니다.";

        log.info("Student {} checked in to session {} with status {}",
            student.getId(), session.getId(), status);

        return new CheckInResponse(message, status);
    }

    public void updateStatus(Long recordId, UpdateAttendanceStatusRequest request) {
        AttendanceRecord record = recordRepository.findById(recordId)
            .orElseThrow(() -> new RecordNotFoundException(recordId));

        record.updateStatus(request.status(), request.memo());
        recordRepository.save(record);

        log.info("Attendance record {} updated to status {}", recordId, request.status());
    }

    public void bulkUpdateStatus(Long sessionId, BulkUpdateStatusRequest request) {
        sessionRepository.findById(sessionId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));

        List<AttendanceRecord> records = recordRepository.findAllById(request.recordIds());

        for (AttendanceRecord record : records) {
            record.updateStatus(request.status(), null);
        }

        recordRepository.saveAll(records);

        log.info("Bulk updated {} records in session {} to status {}",
            records.size(), sessionId, request.status());
    }
}
