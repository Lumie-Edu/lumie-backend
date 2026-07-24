package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.dto.request.GoalSimulationRequest;
import com.lumie.exam.application.dto.response.statistics.*;
import com.lumie.exam.application.service.ChoiceDistributionQueryService;
import com.lumie.exam.application.service.ClassComparisonQueryService;
import com.lumie.exam.application.service.ExamSummaryStatisticsService;
import com.lumie.exam.application.service.ItemAnalysisQueryService;
import com.lumie.exam.application.service.StudentAnalyticsQueryService;
import com.lumie.exam.application.service.StudentGradeQueryService;
import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.api.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<ExamStatisticsResponse> getExamStatistics(
            @PathVariable Long examId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Boolean hasActiveEnrollment,
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(examSummaryStatisticsService.getExamStatistics(
                examId, classId, hasActiveEnrollment, isActive));
    }

    @GetMapping("/exams/{examId}/grades")
    @AllowedSortFields({StudentGradeQueryService.SORT_FIELD_RANK, StudentGradeQueryService.SORT_FIELD_TOTAL_SCORE})
    public ResponseEntity<PageResponse<StudentGradeResponse>> getStudentGrades(
            @PathVariable Long examId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Boolean hasActiveEnrollment,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = "rank") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(studentGradeQueryService.getStudentGrades(
                examId, classId, hasActiveEnrollment, isActive, pageable)));
    }

    @GetMapping("/exams/{examId}/results-summary")
    public ResponseEntity<List<StudentResultSummaryResponse>> getStudentResultSummaries(
            @PathVariable Long examId) {
        return ResponseEntity.ok(studentGradeQueryService.getStudentResultSummaries(examId));
    }

    @GetMapping("/exams/{examId}/choices")
    public ResponseEntity<ChoiceDistributionResponse> getChoiceDistribution(
            @PathVariable Long examId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Boolean hasActiveEnrollment,
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(choiceDistributionQueryService.getChoiceDistribution(
                examId, classId, hasActiveEnrollment, isActive));
    }

    @GetMapping("/exams/{examId}/class-comparison")
    public ResponseEntity<ClassComparisonResponse> getClassComparison(
            @PathVariable Long examId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Boolean hasActiveEnrollment,
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(classComparisonQueryService.getClassComparison(
                examId, classId, hasActiveEnrollment, isActive));
    }

    @GetMapping("/exams/{examId}/item-analysis")
    public ResponseEntity<ItemAnalysisResponse> getItemAnalysis(
            @PathVariable Long examId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Boolean hasActiveEnrollment,
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(itemAnalysisQueryService.getItemAnalysis(
                examId, classId, hasActiveEnrollment, isActive));
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
