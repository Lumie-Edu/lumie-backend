package com.lumie.admin.adapter.in.web;

import com.lumie.admin.application.dto.WorkScheduleRequest;
import com.lumie.admin.application.dto.WorkScheduleResponse;
import com.lumie.admin.application.service.WorkScheduleCommandService;
import com.lumie.admin.application.service.WorkScheduleQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admins/{adminId}/schedules")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final WorkScheduleCommandService workScheduleCommandService;
    private final WorkScheduleQueryService workScheduleQueryService;

    @PostMapping
    public ResponseEntity<List<WorkScheduleResponse>> setWeeklySchedule(
            @PathVariable Long adminId,
            @Valid @RequestBody List<WorkScheduleRequest> requests) {
        List<WorkScheduleResponse> response = workScheduleCommandService.setWeeklySchedule(adminId, requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkScheduleResponse>> getCurrentSchedules(
            @PathVariable Long adminId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate) {
        List<WorkScheduleResponse> response = workScheduleQueryService.getCurrentSchedules(adminId, effectiveDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/calendar")
    public ResponseEntity<List<WorkScheduleResponse>> getCalendarView(@PathVariable Long adminId) {
        List<WorkScheduleResponse> response = workScheduleQueryService.getAllSchedules(adminId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkScheduleResponse> updateSchedule(
            @PathVariable Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody WorkScheduleRequest request) {
        WorkScheduleResponse response = workScheduleCommandService.updateSchedule(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long adminId, @PathVariable Long id) {
        workScheduleCommandService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
