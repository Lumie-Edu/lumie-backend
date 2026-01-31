package com.lumie.content.application.service;

import com.lumie.content.application.dto.request.CreateScheduleRequest;
import com.lumie.content.application.dto.request.UpdateScheduleRequest;
import com.lumie.content.application.dto.response.ScheduleResponse;
import com.lumie.content.domain.entity.Schedule;
import com.lumie.content.domain.exception.ContentErrorCode;
import com.lumie.content.domain.exception.ContentException;
import com.lumie.content.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleCommandService {

    private final ScheduleRepository scheduleRepository;

    @Transactional
    public ScheduleResponse createSchedule(CreateScheduleRequest request) {
        log.info("Creating schedule for date: {}", request.scheduleDate());

        Schedule schedule = Schedule.create(
                request.academyId(),
                request.adminId(),
                request.scheduleDate(),
                request.startTime(),
                request.endTime(),
                request.slotDurationMinutes(),
                request.maxReservations()
        );

        Schedule saved = scheduleRepository.save(schedule);
        log.info("Schedule created with id: {}", saved.getId());

        return ScheduleResponse.from(saved);
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long id, UpdateScheduleRequest request) {
        log.info("Updating schedule: {}", id);

        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ContentException(ContentErrorCode.SCHEDULE_NOT_FOUND));

        schedule.update(
                request.scheduleDate(),
                request.startTime(),
                request.endTime(),
                request.slotDurationMinutes(),
                request.maxReservations()
        );

        if (request.isAvailable() != null) {
            if (request.isAvailable()) {
                schedule.open();
            } else {
                schedule.close();
            }
        }

        Schedule updated = scheduleRepository.save(schedule);
        log.info("Schedule updated: {}", updated.getId());

        return ScheduleResponse.from(updated);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        log.info("Deleting schedule: {}", id);

        if (!scheduleRepository.existsById(id)) {
            throw new ContentException(ContentErrorCode.SCHEDULE_NOT_FOUND);
        }

        scheduleRepository.deleteById(id);
        log.info("Schedule deleted: {}", id);
    }
}
