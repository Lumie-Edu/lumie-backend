package com.lumie.classroom.adapter.in.web;

import com.lumie.common.auth.AuthorizationGuard;

import com.lumie.classroom.application.dto.response.statistics.ClassDashboardResponse;
import com.lumie.classroom.application.service.ClassStatisticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/classes/statistics")
@RequiredArgsConstructor
public class ClassStatisticsController {

    private final ClassStatisticsQueryService classStatisticsQueryService;

    @GetMapping("/dashboard")
    public ResponseEntity<ClassDashboardResponse> getDashboardStatistics() {
        AuthorizationGuard.requireStaff();
        return ResponseEntity.ok(classStatisticsQueryService.getDashboardStatistics());
    }
}
