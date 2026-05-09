package com.lumie.attendance.adapter.in.web;

import com.lumie.attendance.application.service.AttendanceSessionScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/attendance")
@RequiredArgsConstructor
public class AttendanceInternalController {

    private final AttendanceSessionScheduler scheduler;

    @PostMapping("/generate-daily")
    public ResponseEntity<Void> generateDailySessions() {
        scheduler.createDailyAttendanceSessions();
        return ResponseEntity.ok().build();
    }
}
