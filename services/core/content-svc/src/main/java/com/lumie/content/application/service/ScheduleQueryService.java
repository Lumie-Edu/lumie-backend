package com.lumie.content.application.service;

import com.lumie.content.application.dto.response.ScheduleResponse;
import com.lumie.content.domain.entity.Schedule;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.exception.ContentException;
import com.lumie.content.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ScheduleQueryService {

    private final ScheduleRepository scheduleRepository;

    public Page<ScheduleResponse> listSchedules(Pageable pageable) {
        log.debug("Listing schedules with pagination");

        return scheduleRepository.findAll(pageable)
                .map(ScheduleResponse::from);
    }

    public List<ScheduleResponse> listSchedulesByDateRange(LocalDate startDate, LocalDate endDate) {
        log.debug("Listing schedules from {} to {}", startDate, endDate);

        if (startDate.isAfter(endDate)) {
            throw new ContentException(ContentErrorCode.INVALID_DATE_RANGE);
        }

        return scheduleRepository.findByScheduleDateBetween(startDate, endDate).stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    public List<ScheduleResponse> listAvailableSchedules(LocalDate startDate, LocalDate endDate) {
        log.debug("Listing available schedules from {} to {}", startDate, endDate);

        if (startDate.isAfter(endDate)) {
            throw new ContentException(ContentErrorCode.INVALID_DATE_RANGE);
        }

        return scheduleRepository.findByScheduleDateBetweenAndIsAvailableTrue(startDate, endDate).stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    public List<ScheduleResponse> listSchedulesByCounselor(Long counselorId, LocalDate startDate, LocalDate endDate) {
        log.debug("Listing schedules for counselor {} from {} to {}", counselorId, startDate, endDate);

        return scheduleRepository.findByCounselorIdAndScheduleDateBetween(counselorId, startDate, endDate).stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    public ScheduleResponse getSchedule(Long id) {
        log.debug("Getting schedule: {}", id);

        Schedule schedule = scheduleRepository.findByIdWithReservations(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.SCHEDULE_NOT_FOUND));

        return ScheduleResponse.from(schedule);
    }
}
