package com.lumie.academy.adapter.in.web;

import com.lumie.academy.application.dto.response.statistics.AcademyDashboardResponse;
import com.lumie.academy.application.service.AcademyStatisticsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/academies/statistics")
@RequiredArgsConstructor
public class AcademyStatisticsController {

    private final AcademyStatisticsQueryService academyStatisticsQueryService;

    @GetMapping("/dashboard")
    public ResponseEntity<AcademyDashboardResponse> getDashboardStatistics() {
        return ResponseEntity.ok(academyStatisticsQueryService.getDashboardStatistics());
    }
}
