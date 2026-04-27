package com.lumie.attendance.adapter.in.web;

import com.lumie.attendance.application.dto.response.statistics.AttendanceDashboardResponse;
import com.lumie.attendance.application.service.AttendanceStatisticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/attendance/statistics")
@RequiredArgsConstructor
public class AttendanceStatisticsController {

    private final AttendanceStatisticsQueryService attendanceStatisticsQueryService;

    @GetMapping("/dashboard")
    public ResponseEntity<AttendanceDashboardResponse> getDashboardStatistics() {
        return ResponseEntity.ok(attendanceStatisticsQueryService.getDashboardStatistics());
    }
}
