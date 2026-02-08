package com.lumie.admin.adapter.in.web;

import com.lumie.admin.application.dto.AttendanceRequest;
import com.lumie.admin.application.dto.AttendanceResponse;
import com.lumie.admin.application.dto.AttendanceSummaryResponse;
import com.lumie.admin.application.service.AttendanceCommandService;
import com.lumie.admin.application.service.AttendanceQueryService;
import com.lumie.admin.domain.vo.AttendanceStatus;
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
import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceCommandService attendanceCommandService;
    private final AttendanceQueryService attendanceQueryService;

    @PostMapping("/clock-in")
    public ResponseEntity<AttendanceResponse> clockIn(@RequestParam Long adminId) {
        AttendanceResponse response = attendanceCommandService.clockIn(adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/clock-out")
    public ResponseEntity<AttendanceResponse> clockOut(@RequestParam Long adminId) {
        AttendanceResponse response = attendanceCommandService.clockOut(adminId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<AttendanceResponse>> getAttendances(
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) AttendanceStatus status,
            @PageableDefault(size = 20, sort = "workDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AttendanceResponse> response = attendanceQueryService.getAttendances(
                adminId, dateFrom, dateTo, status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttendanceResponse> getAttendance(@PathVariable Long id) {
        AttendanceResponse response = attendanceQueryService.getAttendance(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttendanceResponse> updateAttendance(
            @PathVariable Long id,
            @RequestBody AttendanceRequest request) {
        AttendanceResponse response = attendanceCommandService.updateAttendance(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    public ResponseEntity<AttendanceSummaryResponse> getMonthlySummary(
            @RequestParam Long adminId,
            @RequestParam String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        AttendanceSummaryResponse response = attendanceQueryService.getMonthlySummary(adminId, ym);
        return ResponseEntity.ok(response);
    }
}
