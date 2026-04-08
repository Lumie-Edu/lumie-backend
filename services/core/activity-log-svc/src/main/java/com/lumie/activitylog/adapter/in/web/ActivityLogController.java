package com.lumie.activitylog.adapter.in.web;

import com.lumie.activitylog.application.dto.ActivityLogFilterRequest;
import com.lumie.activitylog.application.dto.ActivityLogResponse;
import com.lumie.activitylog.application.service.ActivityLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/activity-logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogQueryService activityLogQueryService;

    @GetMapping
    public ResponseEntity<Page<ActivityLogResponse>> getLogs(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        ActivityLogFilterRequest filter = new ActivityLogFilterRequest(actorId, entityType, startDate, endDate, search);
        Page<ActivityLogResponse> response = activityLogQueryService.getLogs(filter, pageable);
        return ResponseEntity.ok(response);
    }
}
