package com.lumie.attendance.adapter.in.web;

import com.lumie.attendance.application.dto.CheckInRequest;
import com.lumie.attendance.application.dto.CheckInResponse;
import com.lumie.attendance.application.service.AttendanceRecordCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/attendance")
@RequiredArgsConstructor
public class StudentCheckInController {

    private final AttendanceRecordCommandService commandService;

    @PostMapping("/check-in")
    public ResponseEntity<CheckInResponse> checkIn(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody CheckInRequest request) {
        CheckInResponse response = commandService.checkInByCode(userId, request);
        return ResponseEntity.ok(response);
    }
}
