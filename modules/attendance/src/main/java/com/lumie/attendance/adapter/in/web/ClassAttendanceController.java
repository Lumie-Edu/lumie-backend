package com.lumie.attendance.adapter.in.web;

import com.lumie.attendance.application.dto.response.AttendanceSessionResponse;
import com.lumie.attendance.application.dto.response.ClassAttendanceStatisticsResponse;
import com.lumie.attendance.application.dto.response.ClassStudentAttendanceResponse;
import com.lumie.attendance.application.service.AttendanceRecordQueryService;
import com.lumie.attendance.application.service.AttendanceSessionCommandService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/attendance/classes")
@RequiredArgsConstructor
public class ClassAttendanceController {

    private final AttendanceRecordQueryService queryService;
    private final AttendanceSessionCommandService sessionCommandService;

    @Operation(operationId = "ensureClassSessionToday",
            summary = "Get or create today's attendance session for a class")
    @PostMapping("/{classId}/sessions/ensure")
    public ResponseEntity<AttendanceSessionResponse> ensureSessionToday(@PathVariable Long classId) {
        AttendanceSessionResponse response = sessionCommandService.ensureSessionForClass(classId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{classId}/statistics")
    public ResponseEntity<ClassAttendanceStatisticsResponse> getClassStatistics(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        ClassAttendanceStatisticsResponse response = queryService.getClassStatistics(classId, dateFrom, dateTo);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{classId}/students")
    public ResponseEntity<List<ClassStudentAttendanceResponse>> getClassStudentStatistics(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        List<ClassStudentAttendanceResponse> response = queryService.getClassStudentStatistics(classId, dateFrom, dateTo);
        return ResponseEntity.ok(response);
    }
}
