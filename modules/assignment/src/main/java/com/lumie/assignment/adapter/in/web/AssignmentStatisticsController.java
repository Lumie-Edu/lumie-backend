package com.lumie.assignment.adapter.in.web;

import com.lumie.assignment.application.dto.response.statistics.AssignmentDashboardResponse;
import com.lumie.assignment.application.service.AssignmentStatisticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/assignments/statistics")
@RequiredArgsConstructor
public class AssignmentStatisticsController {

    private final AssignmentStatisticsQueryService assignmentStatisticsQueryService;

    @GetMapping("/dashboard")
    public ResponseEntity<AssignmentDashboardResponse> getDashboardStatistics() {
        return ResponseEntity.ok(assignmentStatisticsQueryService.getDashboardStatistics());
    }
}
