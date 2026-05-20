package com.lumie.attendance.adapter.in.web;

import com.lumie.attendance.application.dto.response.StudentAttendanceRecordResponse;
import com.lumie.attendance.application.dto.response.StudentAttendanceStatisticsResponse;
import com.lumie.attendance.application.service.AttendanceRecordQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/attendance/students")
@RequiredArgsConstructor
public class StudentAttendanceController {

    private final AttendanceRecordQueryService queryService;

    @GetMapping("/{studentId}/records")
    public ResponseEntity<List<StudentAttendanceRecordResponse>> getStudentRecords(
            @PathVariable Long studentId) {
        List<StudentAttendanceRecordResponse> response = queryService.getStudentRecordsWithSession(studentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{studentId}/statistics")
    public ResponseEntity<StudentAttendanceStatisticsResponse> getStudentStatistics(
            @PathVariable Long studentId) {
        StudentAttendanceStatisticsResponse response = queryService.getStudentStatistics(studentId);
        return ResponseEntity.ok(response);
    }
}
