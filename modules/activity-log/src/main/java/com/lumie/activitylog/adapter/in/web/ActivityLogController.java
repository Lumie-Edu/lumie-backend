package com.lumie.activitylog.adapter.in.web;

import com.lumie.activitylog.application.dto.ActivityLogFilterRequest;
import com.lumie.activitylog.application.dto.ActivityLogResponse;
import com.lumie.activitylog.application.service.ActivityLogQueryService;
import com.lumie.common.api.CursorResponse;
import lombok.RequiredArgsConstructor;
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

    private static final int DEFAULT_LIMIT = 30;
    private static final int MAX_LIMIT = 100;

    private final ActivityLogQueryService activityLogQueryService;

    @GetMapping
    public ResponseEntity<CursorResponse<ActivityLogResponse>> getLogs(
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        int requested = limit != null ? limit : DEFAULT_LIMIT;
        int effectiveLimit = Math.min(MAX_LIMIT, Math.max(1, requested));
        ActivityLogFilterRequest filter = new ActivityLogFilterRequest(actorId, entityType, startDate, endDate, search);
        return ResponseEntity.ok(activityLogQueryService.getLogs(filter, cursor, effectiveLimit));
    }
}
