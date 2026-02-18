package com.lumie.attendance.adapter.in.web;

import com.lumie.attendance.application.dto.StudentAttendanceRecordResponse;
import com.lumie.attendance.application.service.AttendanceRecordQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance/students")
@RequiredArgsConstructor
public class StudentAttendanceController {

    private final AttendanceRecordQueryService queryService;

    @GetMapping("/{studentId}/records")
    public ResponseEntity<List<StudentAttendanceRecordResponse>> getStudentRecords(
            @PathVariable Long studentId) {
        List<StudentAttendanceRecordResponse> response = queryService.getStudentRecordsWithSession(studentId);
        return ResponseEntity.ok(response);
    }
}
