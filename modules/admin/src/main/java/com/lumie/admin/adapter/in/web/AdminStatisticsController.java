package com.lumie.admin.adapter.in.web;

import com.lumie.admin.application.dto.response.statistics.AdminDashboardResponse;
import com.lumie.admin.application.service.AdminStatisticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admins/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminStatisticsQueryService adminStatisticsQueryService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboardStatistics() {
        return ResponseEntity.ok(adminStatisticsQueryService.getDashboardStatistics());
    }
}
