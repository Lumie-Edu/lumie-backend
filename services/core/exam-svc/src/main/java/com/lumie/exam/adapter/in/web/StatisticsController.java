package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.dto.request.GoalSimulationRequest;
import com.lumie.exam.application.dto.response.statistics.*;
import com.lumie.exam.application.service.StatisticsQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsQueryService statisticsQueryService;

    /**
     * 시험 전체 통계 조회
     */
    @GetMapping("/exams/{examId}")
    public ResponseEntity<ExamStatisticsResponse> getExamStatistics(@PathVariable Long examId) {
        return ResponseEntity.ok(statisticsQueryService.getExamStatistics(examId));
    }

    /**
     * 시험별 학생 성적 목록 (페이지네이션)
     */
    @GetMapping("/exams/{examId}/grades")
    public ResponseEntity<Page<StudentGradeResponse>> getStudentGrades(
            @PathVariable Long examId,
            @PageableDefault(size = 20, sort = "rank") Pageable pageable) {
        return ResponseEntity.ok(statisticsQueryService.getStudentGrades(examId, pageable));
    }

    /**
     * 선지별 선택률 분석
     */
    @GetMapping("/exams/{examId}/choices")
    public ResponseEntity<ChoiceDistributionResponse> getChoiceDistribution(@PathVariable Long examId) {
        return ResponseEntity.ok(statisticsQueryService.getChoiceDistribution(examId));
    }

    /**
     * 학생 백분위/석차 조회
     */
    @GetMapping("/students/{studentId}/rank")
    public ResponseEntity<StudentRankResponse> getStudentRank(
            @PathVariable Long studentId,
            @RequestParam Long examId) {
        return ResponseEntity.ok(statisticsQueryService.getStudentRank(studentId, examId));
    }

    /**
     * 성적 안정성 지표 조회
     */
    @GetMapping("/students/{studentId}/stability")
    public ResponseEntity<StabilityIndexResponse> getStabilityIndex(@PathVariable Long studentId) {
        return ResponseEntity.ok(statisticsQueryService.getStabilityIndex(studentId));
    }

    /**
     * 유형별 성장 추이 조회
     */
    @GetMapping("/students/{studentId}/type-growth")
    public ResponseEntity<TypeGrowthTrendResponse> getTypeGrowthTrend(@PathVariable Long studentId) {
        return ResponseEntity.ok(statisticsQueryService.getTypeGrowthTrend(studentId));
    }

    /**
     * Z-score 정규화 점수 조회
     */
    @GetMapping("/students/{studentId}/normalized")
    public ResponseEntity<NormalizedScoreResponse> getNormalizedScores(@PathVariable Long studentId) {
        return ResponseEntity.ok(statisticsQueryService.getNormalizedScores(studentId));
    }

    /**
     * 목표 등급 시뮬레이션
     */
    @PostMapping("/students/{studentId}/goal-simulation")
    public ResponseEntity<GoalSimulationResponse> simulateGoal(
            @PathVariable Long studentId,
            @Valid @RequestBody GoalSimulationRequest request) {
        return ResponseEntity.ok(statisticsQueryService.simulateGoal(studentId, request));
    }

    /**
     * 대시보드 통계 조회 (유형별 정답률, 오답률 TOP 10)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatisticsResponse> getDashboardStatistics() {
        return ResponseEntity.ok(statisticsQueryService.getDashboardStatistics());
    }

    /**
     * 학원별 성취도 비교
     */
    @GetMapping("/exams/{examId}/academy-comparison")
    public ResponseEntity<List<AcademyComparisonResponse>> getAcademyComparison(
            @PathVariable Long examId) {
        return ResponseEntity.ok(statisticsQueryService.getAcademyComparison(examId));
    }
}
