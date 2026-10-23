package com.lumie.exam.adapter.in.web;

import com.lumie.common.util.ReportingTime;
import com.lumie.common.api.JobAcceptedResponse;
import com.lumie.common.api.AllowedSortFields;
import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.auth.Role;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.request.OmrBatchConfirmRequest;
import com.lumie.exam.application.dto.request.OmrPresignRequest;
import com.lumie.exam.application.dto.request.OmrUploadTelemetryRequest;
import com.lumie.exam.application.dto.request.ApplyOmrSubmissionRequest;
import com.lumie.exam.application.dto.request.ConfirmOmrSubmissionReconciliationRequest;
import com.lumie.exam.application.dto.request.PreviewOmrSubmissionReconciliationRequest;
import com.lumie.exam.application.dto.request.UpdateOmrSubmissionPhoneRequest;
import com.lumie.exam.application.dto.request.RegisterStudentFromOmrSubmissionRequest;
import com.lumie.exam.application.dto.request.UpdateResultPhoneRequest;
import com.lumie.exam.application.dto.request.UpdateQuestionAnswerRequest;
import com.lumie.exam.application.dto.response.ExamResultResponse;
import com.lumie.exam.application.dto.response.OmrJobResponse;
import com.lumie.exam.application.dto.response.OmrJobStatusResponse;
import com.lumie.exam.application.dto.response.OmrPresignResponse;
import com.lumie.exam.application.dto.response.OmrSubmissionResponse;
import com.lumie.exam.application.dto.response.OmrSubmissionReconciliationPreviewResponse;
import com.lumie.exam.application.dto.response.QuestionResultResponse;
import com.lumie.exam.application.dto.response.statistics.TopStudentMonthlyResponse;
import com.lumie.exam.application.service.ExamExportService;
import com.lumie.exam.application.service.StudentGradeQueryService;
import com.lumie.exam.application.service.ResultCommandService;
import com.lumie.exam.application.service.ResultQueryService;
import com.lumie.exam.application.service.OmrSubmissionService;
import com.lumie.exam.application.service.OmrUploadTelemetryService;
import com.lumie.exam.application.port.out.OmrStoragePort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Sort;
import org.springdoc.core.annotations.ParameterObject;
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
    private static final String OMR_SUBMISSION_REGISTER_STUDENT_SCOPE =
            "POST /v1/exams/{examId}/omr-submissions/{submissionId}/register-student";
    private static final String OMR_SUBMISSION_APPLY_SCOPE =
            "POST /v1/exams/{examId}/omr-submissions/{submissionId}/apply";
    private static final String OMR_SUBMISSION_RECONCILIATION_CONFIRM_SCOPE =
            "POST /v1/exams/{examId}/omr-submissions/{submissionId}/reconciliation/confirm";

    private final ResultCommandService resultCommandService;
    private final ResultQueryService resultQueryService;
    private final OmrSubmissionService omrSubmissionService;
    private final OmrStoragePort omrStoragePort;
    private final ExamExportService examExportService;
    private final IdempotencyService idempotencyService;
    private final OmrUploadTelemetryService omrUploadTelemetryService;

    @PostMapping(value = "/exams/{examId}/results/omr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<ExamResultResponse> processOmrGrading(
            @PathVariable Long examId,
            @RequestParam Long studentId,
            @RequestParam("image") MultipartFile image,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) throws IOException {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        byte[] imageData = image.getBytes();
        if (idempotencyKey != null) {
            return idempotencyService.executeOnceWithoutResponseBody(
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
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        if (idempotencyKey != null) {
            return idempotencyService.executeOnceWithoutResponseBody(
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
    @ApiResponse(
            responseCode = "202",
            description = "Accepted",
            headers = @Header(
                    name = HttpHeaders.LOCATION,
                    description = "OMR grading job status URL",
                    schema = @Schema(type = "string", format = "uri-reference")))
    public ResponseEntity<JobAcceptedResponse> confirmBatchUpload(
            @PathVariable Long examId,
            @Valid @RequestBody OmrBatchConfirmRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
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

    @PostMapping("/exams/{examId}/results/omr/batch/upload-events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordBatchUploadTelemetry(
            @PathVariable Long examId,
            @Valid @RequestBody OmrUploadTelemetryRequest request) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        omrUploadTelemetryService.record(examId, request);
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
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        return ResponseEntity.ok(resultQueryService.getOmrJobStatus(examId, jobId));
    }

    @GetMapping("/exams/{examId}/omr-jobs/{jobId}/results")
    public ResponseEntity<List<ExamResultResponse>> getOmrJobResults(
            @PathVariable Long examId,
            @PathVariable Long jobId) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        return ResponseEntity.ok(resultQueryService.getOmrJobResults(examId, jobId));
    }

    @GetMapping("/exams/{examId}/omr-jobs/{jobId}/submissions")
    public ResponseEntity<List<OmrSubmissionResponse>> getOmrJobSubmissions(
            @PathVariable Long examId,
            @PathVariable Long jobId) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        return ResponseEntity.ok(omrSubmissionService.getJobSubmissions(examId, jobId));
    }

    @GetMapping("/exams/{examId}/omr-submissions/reconciliation")
    public ResponseEntity<List<OmrSubmissionResponse>> getOmrReconciliationQueue(@PathVariable Long examId) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        return ResponseEntity.ok(omrSubmissionService.getReconciliationQueue(examId));
    }

    @PatchMapping("/exams/{examId}/omr-submissions/{submissionId}/phone")
    public ResponseEntity<OmrSubmissionResponse> updateOmrSubmissionPhone(
            @PathVariable Long examId,
            @PathVariable Long submissionId,
            @Valid @RequestBody UpdateOmrSubmissionPhoneRequest request) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        return ResponseEntity.ok(omrSubmissionService.correctPhone(examId, submissionId, request.phoneNumber()));
    }

    @PostMapping("/exams/{examId}/omr-submissions/{submissionId}/apply")
    public ResponseEntity<OmrSubmissionResponse> applyOmrSubmission(
            @PathVariable Long examId,
            @PathVariable Long submissionId,
            @Valid @RequestBody ApplyOmrSubmissionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        if (idempotencyKey != null) {
            return idempotencyService.executeOnceWithoutResponseBody(
                    idempotencyKey,
                    OMR_SUBMISSION_APPLY_SCOPE,
                    java.util.Map.of("examId", examId, "submissionId", submissionId, "request", request),
                    () -> ResponseEntity.ok(omrSubmissionService.apply(examId, submissionId, request))
            );
        }
        return ResponseEntity.ok(omrSubmissionService.apply(examId, submissionId, request));
    }

    @PostMapping("/exams/{examId}/omr-submissions/{submissionId}/reconciliation/preview")
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = OmrSubmissionReconciliationPreviewResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Inactive student phone",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Student not found",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<OmrSubmissionReconciliationPreviewResponse> previewOmrSubmissionReconciliation(
            @PathVariable Long examId,
            @PathVariable Long submissionId,
            @Valid @RequestBody PreviewOmrSubmissionReconciliationRequest request) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        return ResponseEntity.ok(omrSubmissionService.previewReconciliation(examId, submissionId, request));
    }

    @PostMapping("/exams/{examId}/omr-submissions/{submissionId}/reconciliation/confirm")
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = OmrSubmissionResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Inactive student phone or result conflict",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<OmrSubmissionResponse> confirmOmrSubmissionReconciliation(
            @PathVariable Long examId,
            @PathVariable Long submissionId,
            @Valid @RequestBody ConfirmOmrSubmissionReconciliationRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        if (idempotencyKey != null) {
            return idempotencyService.executeOnceWithoutResponseBody(
                    idempotencyKey,
                    OMR_SUBMISSION_RECONCILIATION_CONFIRM_SCOPE,
                    java.util.Map.of("examId", examId, "submissionId", submissionId, "request", request),
                    () -> ResponseEntity.ok(omrSubmissionService.confirmReconciliation(examId, submissionId, request))
            );
        }
        return ResponseEntity.ok(omrSubmissionService.confirmReconciliation(examId, submissionId, request));
    }

    @PostMapping("/exams/{examId}/omr-submissions/{submissionId}/register-student")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(
            responseCode = "409",
            description = "Inactive student phone or duplicate student phone",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<OmrSubmissionResponse> registerStudentFromOmrSubmission(
            @PathVariable Long examId,
            @PathVariable Long submissionId,
            @Valid @RequestBody RegisterStudentFromOmrSubmissionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        if (idempotencyKey != null) {
            return idempotencyService.executeOnceWithoutResponseBody(
                    idempotencyKey,
                    OMR_SUBMISSION_REGISTER_STUDENT_SCOPE,
                    java.util.Map.of("examId", examId, "submissionId", submissionId, "request", request),
                    () -> ResponseEntity.status(HttpStatus.CREATED)
                            .body(omrSubmissionService.registerStudentAndApply(examId, submissionId, request))
            );
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(omrSubmissionService.registerStudentAndApply(examId, submissionId, request));
    }

    @GetMapping("/exams/{examId}/omr-submissions/{submissionId}/image")
    @Operation(responses = @ApiResponse(
            responseCode = "200",
            description = "OMR image",
            content = @Content(
                    mediaType = MediaType.IMAGE_PNG_VALUE,
                    schema = @Schema(type = "string", format = "binary"))))
    public ResponseEntity<byte[]> getOmrSubmissionImage(
            @PathVariable Long examId,
            @PathVariable Long submissionId) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        byte[] imageData = omrSubmissionService.getImage(examId, submissionId, omrStoragePort);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(imageData.length)
                .body(imageData);
    }

    @GetMapping("/exams/{examId}/omr-jobs")
    public ResponseEntity<List<OmrJobStatusResponse>> getOmrJobs(@PathVariable Long examId) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        return ResponseEntity.ok(resultQueryService.getOmrJobs(examId));
    }

    @GetMapping("/exams/{examId}/results")
    public ResponseEntity<List<ExamResultResponse>> getExamResults(@PathVariable Long examId) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        return ResponseEntity.ok(resultQueryService.getExamResults(examId));
    }

    @GetMapping("/students/me/results")
    public ResponseEntity<List<ExamResultResponse>> getCurrentStudentResults() {
        return ResponseEntity.ok(resultQueryService.getCurrentStudentResults());
    }

    @GetMapping("/students/{studentId}/results")
    public ResponseEntity<List<ExamResultResponse>> getStudentResults(@PathVariable Long studentId) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        return ResponseEntity.ok(resultQueryService.getStudentResults(studentId));
    }

    @GetMapping("/exam-results/statistics/top-students-monthly")
    public ResponseEntity<List<TopStudentMonthlyResponse>> getTopStudentsMonthly(
            @RequestParam(defaultValue = "5") int limit) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        return ResponseEntity.ok(resultQueryService.getTopStudentsMonthly(limit));
    }

    @PatchMapping("/question-results/{id}")
    public ResponseEntity<QuestionResultResponse> updateQuestionAnswer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuestionAnswerRequest request) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        QuestionResultResponse response = resultCommandService.updateQuestionAnswer(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/exams/{examId}/results/{resultId}")
    @ApiResponse(responseCode = "204", description = "No Content")
    public ResponseEntity<Void> deleteResult(
            @PathVariable Long examId,
            @PathVariable Long resultId) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        resultCommandService.deleteResult(examId, resultId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/exams/{examId}/results/{resultId}/phone")
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(schema = @Schema(implementation = ExamResultResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Inactive student phone or duplicate result",
            content = @Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<ExamResultResponse> updateResultPhone(
            @PathVariable Long examId,
            @PathVariable Long resultId,
            @Valid @RequestBody UpdateResultPhoneRequest request) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        return ResponseEntity.ok(resultCommandService.updateResultPhone(examId, resultId, request));
    }

    @GetMapping("/results/{resultId}/questions")
    public ResponseEntity<List<QuestionResultResponse>> getQuestionResults(@PathVariable Long resultId) {
        return ResponseEntity.ok(resultQueryService.getQuestionResultsForCurrentUser(resultId));
    }

    @GetMapping("/exams/{examId}/results/{resultId}/omr-image")
    @Operation(responses = @ApiResponse(
            responseCode = "200",
            description = "OMR image",
            content = @Content(
                    mediaType = MediaType.IMAGE_PNG_VALUE,
                    schema = @Schema(type = "string", format = "binary"))))
    public ResponseEntity<byte[]> getOmrImage(
            @PathVariable Long examId,
            @PathVariable Long resultId) {
        byte[] imageData = resultQueryService.getOmrImageData(examId, resultId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .contentLength(imageData.length)
                .body(imageData);
    }

    @GetMapping(value = "/exams/results/export.csv", produces = "text/csv;charset=UTF-8")
    @ApiResponse(responseCode = "200", description = "CSV export", content =
            @Content(mediaType = "text/csv;charset=UTF-8", schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<StreamingResponseBody> exportResultsCsv() {
        AuthorizationGuard.requireOwner();
        String filename = "%s_grades_%s.csv".formatted(
            TenantContextHolder.getRequiredTenant(),
            LocalDate.now(ReportingTime.ZONE).format(DateTimeFormatter.BASIC_ISO_DATE)
        );

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
            .body(examExportService::writeTenantWideCsv);
    }

    @GetMapping(value = "/exams/{examId}/results/export.csv", produces = "text/csv;charset=UTF-8")
    @ApiResponse(responseCode = "200", description = "CSV export", content =
            @Content(mediaType = "text/csv;charset=UTF-8", schema = @Schema(type = "string", format = "binary")))
    @AllowedSortFields({StudentGradeQueryService.SORT_FIELD_RANK, StudentGradeQueryService.SORT_FIELD_TOTAL_SCORE})
    public ResponseEntity<StreamingResponseBody> exportExamResultsCsv(
            @PathVariable Long examId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Boolean hasActiveEnrollment,
            @RequestParam(required = false) Boolean isActive,
            @ParameterObject Sort sort) {
        AuthorizationGuard.requireAtLeast(Role.INSTRUCTOR);
        ExamExportService.CsvExport csvExport = examExportService.prepareCsvForExam(
            examId,
            classId,
            hasActiveEnrollment,
            isActive,
            sort
        );
        String filename = "%s_exam_%d_grades_%s.csv".formatted(
            TenantContextHolder.getRequiredTenant(),
            examId,
            LocalDate.now(ReportingTime.ZONE).format(DateTimeFormatter.BASIC_ISO_DATE)
        );

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
            .body(csvExport::writeTo);
    }
}
