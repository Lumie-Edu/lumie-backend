package com.lumie.attendance.adapter.in.web;

import com.lumie.attendance.application.dto.AttendanceSessionRequest;
import com.lumie.attendance.application.dto.AttendanceSessionResponse;
import com.lumie.attendance.application.service.AttendanceSessionCommandService;
import com.lumie.attendance.application.service.AttendanceSessionQueryService;
import com.lumie.common.api.PageResponse;
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

@RestController
@RequestMapping("/v1/attendance/sessions")
@RequiredArgsConstructor
public class AttendanceSessionController {

    private final AttendanceSessionCommandService commandService;
    private final AttendanceSessionQueryService queryService;

    @PostMapping
    public ResponseEntity<AttendanceSessionResponse> createSession(
            @Valid @RequestBody AttendanceSessionRequest request) {
        AttendanceSessionResponse response = commandService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<AttendanceSessionResponse>> getSessions(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @PageableDefault(size = 20, sort = "sessionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AttendanceSessionResponse> response =
            queryService.getSessions(classId, dateFrom, dateTo, pageable);
        return ResponseEntity.ok(PageResponse.from(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttendanceSessionResponse> getSession(@PathVariable Long id) {
        AttendanceSessionResponse response = queryService.getSession(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/regenerate-code")
    public ResponseEntity<AttendanceSessionResponse> regenerateCode(@PathVariable Long id) {
        AttendanceSessionResponse response = commandService.regenerateCode(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        commandService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
