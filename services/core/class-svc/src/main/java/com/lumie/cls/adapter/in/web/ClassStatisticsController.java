package com.lumie.cls.adapter.in.web;

import com.lumie.cls.application.dto.response.statistics.ClassDashboardResponse;
import com.lumie.cls.application.service.ClassStatisticsQueryService;
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
        return ResponseEntity.ok(classStatisticsQueryService.getDashboardStatistics());
    }
}
