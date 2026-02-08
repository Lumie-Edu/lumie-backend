package com.lumie.attendance.application.service;

import com.lumie.attendance.application.dto.AttendanceSessionResponse;
import com.lumie.attendance.domain.exception.SessionNotFoundException;
import com.lumie.attendance.domain.repository.AttendanceSessionRepository;
import com.lumie.attendance.domain.vo.SessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceSessionQueryService {

    private final AttendanceSessionRepository sessionRepository;

    public AttendanceSessionResponse getSession(Long id) {
        return sessionRepository.findById(id)
            .map(AttendanceSessionResponse::from)
            .orElseThrow(() -> new SessionNotFoundException(id));
    }

    public Page<AttendanceSessionResponse> getSessions(Long academyId, LocalDate dateFrom,
                                                        LocalDate dateTo, SessionStatus status,
                                                        Pageable pageable) {
        return sessionRepository.search(academyId, dateFrom, dateTo, status, pageable)
            .map(AttendanceSessionResponse::from);
    }
}
