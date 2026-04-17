package com.lumie.attendance.adapter.in.web;

import com.lumie.attendance.application.dto.ClassAttendanceStatisticsResponse;
import com.lumie.attendance.application.dto.ClassStudentAttendanceResponse;
import com.lumie.attendance.application.service.AttendanceRecordQueryService;
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
