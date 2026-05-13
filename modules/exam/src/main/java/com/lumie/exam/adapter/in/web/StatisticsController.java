package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.dto.request.GoalSimulationRequest;
import com.lumie.exam.application.dto.response.statistics.*;
import com.lumie.exam.application.service.ChoiceDistributionQueryService;
import com.lumie.exam.application.service.ClassComparisonQueryService;
import com.lumie.exam.application.service.ExamSummaryStatisticsService;
import com.lumie.exam.application.service.ItemAnalysisQueryService;
import com.lumie.exam.application.service.StudentAnalyticsQueryService;
import com.lumie.exam.application.service.StudentGradeQueryService;
import com.lumie.common.api.PageResponse;
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

    private final ExamSummaryStatisticsService examSummaryStatisticsService;
    private final ChoiceDistributionQueryService choiceDistributionQueryService;
    private final ClassComparisonQueryService classComparisonQueryService;
    private final ItemAnalysisQueryService itemAnalysisQueryService;
    private final StudentAnalyticsQueryService studentAnalyticsQueryService;
    private final StudentGradeQueryService studentGradeQueryService;

    @GetMapping("/exams/{examId}")
    public ResponseEntity<ExamStatisticsResponse> getExamStatistics(@PathVariable Long examId) {
        return ResponseEntity.ok(examSummaryStatisticsService.getExamStatistics(examId));
    }

    @GetMapping("/exams/{examId}/grades")
    public ResponseEntity<PageResponse<StudentGradeResponse>> getStudentGrades(
            @PathVariable Long examId,
            @PageableDefault(size = 20, sort = "rank") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(studentGradeQueryService.getStudentGrades(examId, pageable)));
    }

    @GetMapping("/exams/{examId}/results-summary")
    public ResponseEntity<List<StudentResultSummaryResponse>> getStudentResultSummaries(
            @PathVariable Long examId) {
        return ResponseEntity.ok(studentGradeQueryService.getStudentResultSummaries(examId));
    }

    @GetMapping("/exams/{examId}/choices")
    public ResponseEntity<ChoiceDistributionResponse> getChoiceDistribution(@PathVariable Long examId) {
        return ResponseEntity.ok(choiceDistributionQueryService.getChoiceDistribution(examId));
    }

    @GetMapping("/exams/{examId}/class-comparison")
    public ResponseEntity<ClassComparisonResponse> getClassComparison(@PathVariable Long examId) {
        return ResponseEntity.ok(classComparisonQueryService.getClassComparison(examId));
    }

    @GetMapping("/exams/{examId}/item-analysis")
    public ResponseEntity<ItemAnalysisResponse> getItemAnalysis(@PathVariable Long examId) {
        return ResponseEntity.ok(itemAnalysisQueryService.getItemAnalysis(examId));
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
        return ResponseEntity.ok(examSummaryStatisticsService.getDashboardStatistics());
    }
}
