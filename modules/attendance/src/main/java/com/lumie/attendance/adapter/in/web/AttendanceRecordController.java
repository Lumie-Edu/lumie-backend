package com.lumie.attendance.adapter.in.web;

import com.lumie.attendance.application.dto.AttendanceRecordResponse;
import com.lumie.attendance.application.dto.BulkUpdateStatusRequest;
import com.lumie.attendance.application.dto.UpdateAttendanceStatusRequest;
import com.lumie.attendance.application.service.AttendanceRecordCommandService;
import com.lumie.attendance.application.service.AttendanceRecordQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/attendance/sessions/{sessionId}/records")
@RequiredArgsConstructor
public class AttendanceRecordController {

    private final AttendanceRecordCommandService commandService;
    private final AttendanceRecordQueryService queryService;

    @GetMapping
    public ResponseEntity<List<AttendanceRecordResponse>> getRecords(@PathVariable Long sessionId) {
        List<AttendanceRecordResponse> response = queryService.getRecordsBySession(sessionId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{recordId}")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long sessionId,
            @PathVariable Long recordId,
            @Valid @RequestBody UpdateAttendanceStatusRequest request) {
        commandService.updateStatus(recordId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-update")
    public ResponseEntity<Void> bulkUpdateStatus(
            @PathVariable Long sessionId,
            @Valid @RequestBody BulkUpdateStatusRequest request) {
        commandService.bulkUpdateStatus(sessionId, request);
        return ResponseEntity.noContent().build();
    }
}
