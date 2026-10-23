package com.lumie.staff.adapter.in.web;

import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.auth.Role;
import com.lumie.staff.application.dto.response.statistics.StaffDashboardResponse;
import com.lumie.staff.application.service.StaffStatisticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/staff/statistics")
@RequiredArgsConstructor
public class StaffStatisticsController {

    private final StaffStatisticsQueryService staffStatisticsQueryService;

    @GetMapping("/dashboard")
    public ResponseEntity<StaffDashboardResponse> getDashboardStatistics() {
        AuthorizationGuard.requireAtLeast(Role.MANAGER);
        return ResponseEntity.ok(staffStatisticsQueryService.getDashboardStatistics());
    }
}
