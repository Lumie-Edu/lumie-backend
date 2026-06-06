package com.lumie.attendance.application.service;

import com.lumie.attendance.application.dto.response.AttendanceSessionResponse;
import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.exception.AttendanceErrorCode;
import com.lumie.attendance.domain.repository.AttendanceRecordRepository;
import com.lumie.common.exception.BusinessException;
import com.lumie.attendance.domain.repository.AttendanceSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceSessionQueryService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;

    public AttendanceSessionResponse getSession(Long id) {
        return sessionRepository.findById(id)
            .map(AttendanceSessionResponse::from)
            .orElseThrow(() -> new BusinessException(AttendanceErrorCode.SESSION_NOT_FOUND,
                "Attendance session not found with id: %d".formatted(id)));
    }

    public Page<AttendanceSessionResponse> getSessions(Long classId,
                                                        LocalDate dateFrom, LocalDate dateTo,
                                                        Pageable pageable) {
        Page<AttendanceSession> sessions = sessionRepository.search(classId, dateFrom, dateTo, pageable);
        List<Long> sessionIds = sessions.stream()
            .map(AttendanceSession::getId)
            .toList();

        if (sessionIds.isEmpty()) {
            return sessions.map(AttendanceSessionResponse::fromWithoutRecords);
        }

        Map<Long, SessionCounts> countsBySessionId = new HashMap<>();
        for (AttendanceRecordRepository.SessionStatusCount row :
                recordRepository.countBySessionIdsGroupedByStatus(sessionIds)) {
            SessionCounts counts = countsBySessionId.computeIfAbsent(row.getSessionId(), ignored -> new SessionCounts());
            counts.add(row.getStatus(), Math.toIntExact(row.getRecordCount()));
        }

        return sessions.map(session -> {
            SessionCounts counts = countsBySessionId.getOrDefault(session.getId(), SessionCounts.EMPTY);
            return AttendanceSessionResponse.fromCounts(
                session,
                counts.presentCount,
                counts.absentCount,
                counts.lateCount,
                counts.excusedCount,
                counts.totalStudents
            );
        });
    }

    private static class SessionCounts {
        private static final SessionCounts EMPTY = new SessionCounts();

        private int presentCount;
        private int absentCount;
        private int lateCount;
        private int excusedCount;
        private int totalStudents;

        private void add(com.lumie.attendance.domain.vo.AttendanceStatus status, int count) {
            totalStudents += count;
            switch (status) {
                case PRESENT -> presentCount += count;
                case ABSENT -> absentCount += count;
                case LATE -> lateCount += count;
                case EXCUSED -> excusedCount += count;
                case PENDING -> {
                    // Included in totalStudents only.
                }
            }
        }
    }
}
