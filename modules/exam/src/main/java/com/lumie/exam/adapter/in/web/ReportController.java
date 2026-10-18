package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.dto.request.ReportBatchRequest;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.lumie.exam.application.dto.response.ReportJobStatusResponse;
import com.lumie.exam.application.port.out.ReportServicePort;
import com.lumie.exam.application.service.ReportJobService;
import com.lumie.common.api.JobAcceptedResponse;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.TenantContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ReportController {

    private static final String REPORTS_BATCH_SCOPE = "POST /v1/exams/{examId}/reports/batch";

    private final ReportServicePort reportServicePort;
    private final ReportJobService reportJobService;
    private final IdempotencyService idempotencyService;

    @GetMapping(value = "/reports/students/{studentId}/exams/{examId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateReport(
            @PathVariable Long studentId,
            @PathVariable Long examId
    ) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        byte[] reportBytes = reportServicePort.generateReport(studentId, examId, tenantSlug);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.inline()
                        .filename(reportJobService.getSingleReportFilename(studentId, examId), StandardCharsets.UTF_8)
                        .build()
                        .toString());
        // Generation is expensive (worker call) — prevent client/proxy re-fetch on every render.
        // Server-side is naturally idempotent for (studentId, examId), so caching would be safe,
        // but no-store keeps the simplest contract until a real caching strategy is decided.
        headers.setCacheControl("no-store");

        return ResponseEntity.ok()
                .headers(headers)
                .body(reportBytes);
    }

    @PostMapping("/exams/{examId}/reports/batch")
    @ApiResponse(responseCode = "202", description = "Accepted")
    public ResponseEntity<JobAcceptedResponse> createBatchReport(
            @PathVariable Long examId,
            @Valid @RequestBody ReportBatchRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        if (idempotencyKey != null) {
            return idempotencyService.executeOnce(
                    idempotencyKey,
                    REPORTS_BATCH_SCOPE,
                    java.util.Map.of("examId", examId, "request", request),
                    () -> acceptedReportJob(examId, reportJobService.createBatchJob(examId, request.studentIds()))
            );
        }
        Long jobId = reportJobService.createBatchJob(examId, request.studentIds());
        return acceptedReportJob(examId, jobId);
    }

    private ResponseEntity<JobAcceptedResponse> acceptedReportJob(Long examId, Long jobId) {
        String statusUrl = "/v1/exams/" + examId + "/reports/jobs/" + jobId;
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(URI.create(statusUrl))
                .body(new JobAcceptedResponse(jobId, statusUrl));
    }

    @GetMapping("/exams/{examId}/reports/jobs/{jobId}")
    public ResponseEntity<ReportJobStatusResponse> getJobStatus(
            @PathVariable Long examId,
            @PathVariable Long jobId
    ) {
        return ResponseEntity.ok(reportJobService.getJobStatus(jobId));
    }

    @GetMapping(value = "/exams/{examId}/reports/jobs/{jobId}/download", produces = "application/zip")
    public ResponseEntity<byte[]> downloadZip(
            @PathVariable Long examId,
            @PathVariable Long jobId
    ) {
        byte[] zipBytes = reportJobService.getZipBytes(jobId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                        .filename(reportJobService.getZipFilename(jobId), StandardCharsets.UTF_8)
                        .build()
                        .toString());

        return ResponseEntity.ok()
                .headers(headers)
                .body(zipBytes);
    }
}
