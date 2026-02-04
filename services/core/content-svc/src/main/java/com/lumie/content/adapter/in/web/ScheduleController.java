package com.lumie.content.adapter.in.web;

import com.lumie.content.application.dto.request.CreateScheduleRequest;
import com.lumie.content.application.dto.request.UpdateScheduleRequest;
import com.lumie.content.application.dto.response.ScheduleResponse;
import com.lumie.content.application.service.ScheduleCommandService;
import com.lumie.content.application.service.ScheduleQueryService;
import com.lumie.common.tenant.UserContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleCommandService commandService;
    private final ScheduleQueryService queryService;

    @GetMapping
    public ResponseEntity<Page<ScheduleResponse>> listSchedules(
            @PageableDefault(size = 20, sort = "date", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(queryService.listSchedules(pageable));
    }

    @GetMapping("/range")
    public ResponseEntity<List<ScheduleResponse>> listSchedulesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(queryService.listSchedulesByDateRange(startDate, endDate));
    }

    @GetMapping("/available")
    public ResponseEntity<List<ScheduleResponse>> listAvailableSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(queryService.listAvailableSchedules(startDate, endDate));
    }

    @GetMapping("/admin/{adminId}")
    public ResponseEntity<List<ScheduleResponse>> listSchedulesByAdmin(
            @PathVariable Long adminId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(queryService.listSchedulesByAdmin(adminId, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponse> getSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(queryService.getSchedule(id));
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(@Valid @RequestBody CreateScheduleRequest request) {
        Long adminId = UserContextHolder.getRequiredUserId();
        ScheduleResponse response = commandService.createSchedule(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateScheduleRequest request) {
        return ResponseEntity.ok(commandService.updateSchedule(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        commandService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
