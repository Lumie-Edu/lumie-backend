package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.dto.response.ExamResultResponse;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.exam.application.dto.response.QuestionResultResponse;
import com.lumie.exam.application.dto.request.ReportBatchDataRequest;
import com.lumie.exam.application.dto.response.ReportBatchDataResponse;
import com.lumie.exam.application.dto.response.statistics.ExamStatisticsResponse;
import com.lumie.exam.application.dto.response.statistics.StabilityIndexResponse;
import com.lumie.exam.application.dto.response.statistics.StudentRankResponse;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.service.*;
import com.lumie.student.api.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/internal/reports")
@RequiredArgsConstructor
public class InternalReportController {

    private final ExamSummaryStatisticsService examSummaryStatisticsService;
    private final StudentAnalyticsQueryService studentAnalyticsQueryService;
    private final ResultQueryService resultQueryService;
    private final StudentService studentService;
    private final ReportBatchDataQueryService reportBatchDataQueryService;

    @GetMapping("/exams/{examId}/statistics")
    public ResponseEntity<ExamStatisticsResponse> getExamStatistics(@PathVariable Long examId) {
        return ResponseEntity.ok(examSummaryStatisticsService.getExamStatistics(examId));
    }

    @PostMapping("/exams/{examId}/batch-data")
    public ResponseEntity<ReportBatchDataResponse> getReportBatchData(
            @PathVariable Long examId,
            @Valid @RequestBody ReportBatchDataRequest request) {
        return ResponseEntity.ok(reportBatchDataQueryService.getBatchData(examId, request.studentIds(), request.jobId()));
    }

    @GetMapping("/students/{studentId}/stability")
    public ResponseEntity<StabilityIndexResponse> getStabilityIndex(@PathVariable Long studentId) {
        return ResponseEntity.ok(studentAnalyticsQueryService.getStabilityIndex(studentId));
    }

    @GetMapping("/students/{studentId}/rank")
    public ResponseEntity<StudentRankResponse> getStudentRank(
            @PathVariable Long studentId, @RequestParam Long examId) {
        return ResponseEntity.ok(studentAnalyticsQueryService.getStudentRank(studentId, examId));
    }

    @GetMapping("/students/{studentId}/results")
    public ResponseEntity<List<ExamResultResponse>> getStudentResults(@PathVariable Long studentId) {
        return ResponseEntity.ok(resultQueryService.getStudentResults(studentId));
    }

    @GetMapping("/results/{resultId}/questions")
    public ResponseEntity<List<QuestionResultResponse>> getQuestionResults(@PathVariable Long resultId) {
        return ResponseEntity.ok(resultQueryService.getQuestionResults(resultId));
    }

    @GetMapping("/students/{studentId}")
    public ResponseEntity<StudentService.StudentData> getStudent(@PathVariable Long studentId) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        return studentService.getStudent(tenantSlug, studentId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Student", String.valueOf(studentId)));
    }
}
