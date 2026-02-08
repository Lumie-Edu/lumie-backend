package com.lumie.admin.application.service;

import com.lumie.admin.application.dto.WorkScheduleRequest;
import com.lumie.admin.application.dto.WorkScheduleResponse;
import com.lumie.admin.domain.entity.Admin;
import com.lumie.admin.domain.entity.WorkSchedule;
import com.lumie.admin.domain.exception.AdminNotFoundException;
import com.lumie.admin.domain.exception.WorkScheduleNotFoundException;
import com.lumie.admin.domain.repository.AdminRepository;
import com.lumie.admin.domain.repository.WorkScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WorkScheduleCommandService {

    private final WorkScheduleRepository workScheduleRepository;
    private final AdminRepository adminRepository;

    public List<WorkScheduleResponse> setWeeklySchedule(Long adminId, List<WorkScheduleRequest> requests) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new AdminNotFoundException(adminId));

        List<WorkScheduleResponse> responses = new ArrayList<>();
        for (WorkScheduleRequest request : requests) {
            WorkSchedule schedule = WorkSchedule.builder()
                    .admin(admin)
                    .dayOfWeek(request.dayOfWeek())
                    .startTime(request.startTime())
                    .endTime(request.endTime())
                    .breakStartTime(request.breakStartTime())
                    .breakEndTime(request.breakEndTime())
                    .effectiveFrom(request.effectiveFrom())
                    .effectiveTo(request.effectiveTo())
                    .isDayOff(request.isDayOff())
                    .build();
            WorkSchedule saved = workScheduleRepository.save(schedule);
            responses.add(WorkScheduleResponse.from(saved));
        }

        log.info("Weekly schedule set for admin {}: {} entries", adminId, requests.size());
        return responses;
    }

    public WorkScheduleResponse updateSchedule(Long id, WorkScheduleRequest request) {
        WorkSchedule schedule = workScheduleRepository.findById(id)
                .orElseThrow(() -> new WorkScheduleNotFoundException(id));
        schedule.update(request.startTime(), request.endTime(),
                request.breakStartTime(), request.breakEndTime(),
                request.effectiveTo(), request.isDayOff());
        WorkSchedule updated = workScheduleRepository.save(schedule);
        log.info("Schedule updated: {}", id);
        return WorkScheduleResponse.from(updated);
    }

    public void deleteSchedule(Long id) {
        WorkSchedule schedule = workScheduleRepository.findById(id)
                .orElseThrow(() -> new WorkScheduleNotFoundException(id));
        workScheduleRepository.delete(schedule);
        log.info("Schedule deleted: {}", id);
    }
}
