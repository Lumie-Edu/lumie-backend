package com.lumie.exam.adapter.in.web;

import com.lumie.common.api.JobAcceptedResponse;
import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.request.OmrBatchConfirmRequest;
import com.lumie.exam.application.dto.request.OmrPresignRequest;
import com.lumie.exam.application.dto.request.UpdateQuestionAnswerRequest;
import com.lumie.exam.application.dto.response.ExamResultResponse;
import com.lumie.exam.application.dto.response.OmrJobResponse;
import com.lumie.exam.application.dto.response.OmrJobStatusResponse;
import com.lumie.exam.application.dto.response.OmrPresignResponse;
import com.lumie.exam.application.dto.response.QuestionResultResponse;
import com.lumie.exam.application.dto.response.statistics.TopStudentMonthlyResponse;
import com.lumie.exam.application.service.ExamExportService;
import com.lumie.exam.application.service.ResultCommandService;
import com.lumie.exam.application.service.ResultQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ResultController {

    private static final String OMR_SINGLE_SCOPE = "POST /v1/exams/{examId}/results/omr";
    private static final String OMR_BATCH_PRESIGN_SCOPE = "POST /v1/exams/{examId}/results/omr/batch/presign";
    private static final String OMR_BATCH_CONFIRM_SCOPE = "POST /v1/exams/{examId}/results/omr/batch/confirm";

    private final ResultCommandService resultCommandService;
    private final ResultQueryService resultQueryService;
    private final ExamExportService examExportService;
    private final IdempotencyService idempotencyService;

    @PostMapping(value = "/exams/{examId}/results/omr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExamResultResponse> processOmrGrading(
            @PathVariable Long examId,
            @RequestParam Long studentId,
            @RequestParam("image") MultipartFile image,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) throws IOException {
        byte[] imageData = image.getBytes();
        if (idempotencyKey != null) {
            return idempotencyService.executeOnce(
                    idempotencyKey,
                    OMR_SINGLE_SCOPE,
                    java.util.Map.of("examId", examId, "studentId", studentId),
                    () -> ResponseEntity.status(HttpStatus.CREATED)
                            .body(resultCommandService.processOmrGrading(examId, studentId, imageData))
            );
        }
        ExamResultResponse response = resultCommandService.processOmrGrading(examId, studentId, imageData);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/exams/{examId}/results/omr/batch/presign")
    public ResponseEntity<OmrPresignResponse> requestPresignedUrls(
            @PathVariable Long examId,
            @Valid @RequestBody OmrPresignRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null) {
            return idempotencyService.executeOnce(
                    idempotencyKey,
                    OMR_BATCH_PRESIGN_SCOPE,
                    java.util.Map.of("examId", examId, "request", request),
                    () -> ResponseEntity.ok(resultCommandService.generatePresignedUrls(examId, request))
            );
        }
        OmrPresignResponse response = resultCommandService.generatePresignedUrls(examId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/exams/{examId}/results/omr/batch/confirm")
    public ResponseEntity<JobAcceptedResponse> confirmBatchUpload(
            @PathVariable Long examId,
            @Valid @RequestBody OmrBatchConfirmRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (idempotencyKey != null) {
            return idempotencyService.executeOnce(
                    idempotencyKey,
                    OMR_BATCH_CONFIRM_SCOPE,
                    java.util.Map.of("examId", examId, "request", request),
                    () -> acceptedOmrJob(examId, resultCommandService.confirmBatchOmrGrading(examId, request))
            );
        }
        OmrJobResponse response = resultCommandService.confirmBatchOmrGrading(examId, request);
        return acceptedOmrJob(examId, response);
    }

    private ResponseEntity<JobAcceptedResponse> acceptedOmrJob(Long examId, OmrJobResponse response) {
        String statusUrl = "/v1/exams/" + examId + "/omr-jobs/" + response.jobId();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(URI.create(statusUrl))
                .body(new JobAcceptedResponse(response.jobId(), statusUrl));
    }

    @GetMapping("/exams/{examId}/omr-jobs/{jobId}")
    public ResponseEntity<OmrJobStatusResponse> getOmrJobStatus(
            @PathVariable Long examId,
            @PathVariable Long jobId) {
        return ResponseEntity.ok(resultQueryService.getOmrJobStatus(examId, jobId));
    }

    @GetMapping("/exams/{examId}/omr-jobs/{jobId}/results")
    public ResponseEntity<List<ExamResultResponse>> getOmrJobResults(
            @PathVariable Long examId,
            @PathVariable Long jobId) {
        return ResponseEntity.ok(resultQueryService.getOmrJobResults(examId, jobId));
    }

    @GetMapping("/exams/{examId}/omr-jobs")
    public ResponseEntity<List<OmrJobStatusResponse>> getOmrJobs(@PathVariable Long examId) {
        return ResponseEntity.ok(resultQueryService.getOmrJobs(examId));
    }

    @GetMapping("/exams/{examId}/results")
    public ResponseEntity<List<ExamResultResponse>> getExamResults(@PathVariable Long examId) {
        return ResponseEntity.ok(resultQueryService.getExamResults(examId));
    }

    @GetMapping("/students/{studentId}/results")
    public ResponseEntity<List<ExamResultResponse>> getStudentResults(@PathVariable Long studentId) {
        return ResponseEntity.ok(resultQueryService.getStudentResults(studentId));
    }

    @GetMapping("/exam-results/statistics/top-students-monthly")
    public ResponseEntity<List<TopStudentMonthlyResponse>> getTopStudentsMonthly(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(resultQueryService.getTopStudentsMonthly(limit));
    }

    @PatchMapping("/question-results/{id}")
    public ResponseEntity<QuestionResultResponse> updateQuestionAnswer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuestionAnswerRequest request) {
        QuestionResultResponse response = resultCommandService.updateQuestionAnswer(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/exams/{examId}/results/{resultId}")
    public ResponseEntity<Void> deleteResult(
            @PathVariable Long examId,
            @PathVariable Long resultId) {
        resultCommandService.deleteResult(examId, resultId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/results/{resultId}/questions")
    public ResponseEntity<List<QuestionResultResponse>> getQuestionResults(@PathVariable Long resultId) {
        return ResponseEntity.ok(resultQueryService.getQuestionResults(resultId));
    }

    @GetMapping("/exams/{examId}/results/{resultId}/omr-image")
    public ResponseEntity<byte[]> getOmrImage(
            @PathVariable Long examId,
            @PathVariable Long resultId) {
        byte[] imageData = resultQueryService.getOmrImageData(resultId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(imageData.length)
                .body(imageData);
    }

    @GetMapping(value = "/exams/results/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<StreamingResponseBody> exportResultsCsv() {
        AuthorizationGuard.requireOwner();
        String filename = "%s_grades_%s.csv".formatted(
            TenantContextHolder.getRequiredTenant(),
            LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        );

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
            .body(examExportService::writeCsv);
    }
}
