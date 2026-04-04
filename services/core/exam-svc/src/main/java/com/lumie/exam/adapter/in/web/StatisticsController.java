package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.dto.request.GoalSimulationRequest;
import com.lumie.exam.application.dto.response.statistics.*;
import com.lumie.exam.application.service.ExamStatisticsQueryService;
import com.lumie.exam.application.service.StudentAnalyticsQueryService;
import com.lumie.exam.application.service.StudentGradeQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final ExamStatisticsQueryService examStatisticsQueryService;
    private final StudentAnalyticsQueryService studentAnalyticsQueryService;
    private final StudentGradeQueryService studentGradeQueryService;

    @GetMapping("/exams/{examId}")
    public ResponseEntity<ExamStatisticsResponse> getExamStatistics(@PathVariable Long examId) {
        return ResponseEntity.ok(examStatisticsQueryService.getExamStatistics(examId));
    }

    @GetMapping("/exams/{examId}/grades")
    public ResponseEntity<Page<StudentGradeResponse>> getStudentGrades(
            @PathVariable Long examId,
            @PageableDefault(size = 20, sort = "rank") Pageable pageable) {
        return ResponseEntity.ok(studentGradeQueryService.getStudentGrades(examId, pageable));
    }

    @GetMapping("/exams/{examId}/results-summary")
    public ResponseEntity<List<StudentResultSummaryResponse>> getStudentResultSummaries(
            @PathVariable Long examId) {
        return ResponseEntity.ok(studentGradeQueryService.getStudentResultSummaries(examId));
    }

    @GetMapping("/exams/{examId}/choices")
    public ResponseEntity<ChoiceDistributionResponse> getChoiceDistribution(@PathVariable Long examId) {
        return ResponseEntity.ok(examStatisticsQueryService.getChoiceDistribution(examId));
    }

    @GetMapping("/students/{studentId}/rank")
    public ResponseEntity<StudentRankResponse> getStudentRank(
            @PathVariable Long studentId,
            @RequestParam Long examId) {
        return ResponseEntity.ok(studentAnalyticsQueryService.getStudentRank(studentId, examId));
    }

    @GetMapping("/students/{studentId}/stability")
    public ResponseEntity<StabilityIndexResponse> getStabilityIndex(@PathVariable Long studentId) {
        return ResponseEntity.ok(studentAnalyticsQueryService.getStabilityIndex(studentId));
    }

    @GetMapping("/students/{studentId}/type-growth")
    public ResponseEntity<TypeGrowthTrendResponse> getTypeGrowthTrend(@PathVariable Long studentId) {
        return ResponseEntity.ok(studentAnalyticsQueryService.getTypeGrowthTrend(studentId));
    }

    @GetMapping("/students/{studentId}/normalized")
    public ResponseEntity<NormalizedScoreResponse> getNormalizedScores(@PathVariable Long studentId) {
        return ResponseEntity.ok(studentAnalyticsQueryService.getNormalizedScores(studentId));
    }

    @PostMapping("/students/{studentId}/goal-simulation")
    public ResponseEntity<GoalSimulationResponse> simulateGoal(
            @PathVariable Long studentId,
            @Valid @RequestBody GoalSimulationRequest request) {
        return ResponseEntity.ok(studentAnalyticsQueryService.simulateGoal(studentId, request));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatisticsResponse> getDashboardStatistics() {
        return ResponseEntity.ok(examStatisticsQueryService.getDashboardStatistics());
    }

    @GetMapping("/exams/{examId}/academy-comparison")
    public ResponseEntity<List<AcademyComparisonResponse>> getAcademyComparison(
            @PathVariable Long examId) {
        return ResponseEntity.ok(examStatisticsQueryService.getAcademyComparison(examId));
    }
}
