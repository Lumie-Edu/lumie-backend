package com.lumie.admin.application.service;

import com.lumie.admin.application.dto.WorkScheduleResponse;
import com.lumie.admin.domain.repository.WorkScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkScheduleQueryService {

    private final WorkScheduleRepository workScheduleRepository;

    public List<WorkScheduleResponse> getCurrentSchedules(Long adminId, LocalDate effectiveDate) {
        LocalDate date = effectiveDate != null ? effectiveDate : LocalDate.now();
        return workScheduleRepository.findByAdminIdAndEffectiveDate(adminId, date).stream()
                .map(WorkScheduleResponse::from)
                .toList();
    }

    public List<WorkScheduleResponse> getAllSchedules(Long adminId) {
        return workScheduleRepository.findByAdminId(adminId).stream()
                .map(WorkScheduleResponse::from)
                .toList();
    }
}
