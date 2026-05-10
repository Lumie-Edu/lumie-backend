package com.lumie.exam.adapter.in.web;

import com.lumie.exam.application.dto.request.ReportBatchRequest;
import com.lumie.exam.application.dto.response.ReportJobResponse;
import com.lumie.exam.application.dto.response.ReportJobStatusResponse;
import com.lumie.exam.application.port.out.ReportServicePort;
import com.lumie.exam.application.service.ReportJobService;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ReportController {

    private static final String REPORTS_BATCH_SCOPE = "POST /v1/exams/{examId}/reports/batch";

    private final ReportServicePort reportServicePort;
    private final ReportJobService reportJobService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/reports/students/{studentId}/exams/{examId}")
    public ResponseEntity<byte[]> generateReport(
            @PathVariable Long studentId,
            @PathVariable Long examId
    ) {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        byte[] jpgBytes = reportServicePort.generateReport(studentId, examId, tenantSlug);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentDispositionFormData(
                "inline",
                String.format("report_%d_%d.jpg", studentId, examId)
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(jpgBytes);
    }

    @PostMapping("/exams/{examId}/reports/batch")
    public ResponseEntity<ReportJobResponse> createBatchReport(
            @PathVariable Long examId,
            @RequestBody ReportBatchRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        if (idempotencyKey != null) {
            return idempotencyService.executeOnce(
                    idempotencyKey,
                    REPORTS_BATCH_SCOPE,
                    java.util.Map.of("examId", examId, "request", request),
                    () -> {
                        Long jobId = reportJobService.createBatchJob(examId, request.studentIds());
                        return ResponseEntity.ok(new ReportJobResponse(jobId));
                    }
            );
        }
        Long jobId = reportJobService.createBatchJob(examId, request.studentIds());
        return ResponseEntity.ok(new ReportJobResponse(jobId));
    }

    @GetMapping("/exams/{examId}/reports/jobs/{jobId}")
    public ResponseEntity<ReportJobStatusResponse> getJobStatus(
            @PathVariable Long examId,
            @PathVariable Long jobId
    ) {
        return ResponseEntity.ok(reportJobService.getJobStatus(jobId));
    }

    @GetMapping("/exams/{examId}/reports/jobs/{jobId}/download")
    public ResponseEntity<byte[]> downloadZip(
            @PathVariable Long examId,
            @PathVariable Long jobId
    ) {
        byte[] zipBytes = reportJobService.getZipBytes(jobId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.setContentDispositionFormData("attachment",
                String.format("reports_%d_%d.zip", examId, jobId));

        return ResponseEntity.ok()
                .headers(headers)
                .body(zipBytes);
    }
}
