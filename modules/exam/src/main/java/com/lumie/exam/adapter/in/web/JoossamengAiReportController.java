package com.lumie.exam.adapter.in.web;

import com.lumie.common.api.JobAcceptedResponse;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.adapter.out.external.JoossamengAiReportClient;
import com.lumie.exam.application.dto.request.ReportBatchRequest;
import com.lumie.exam.application.dto.response.AiReportCreateResponse;
import com.lumie.exam.application.dto.response.AiReportJobStatusResponse;
import com.lumie.exam.domain.exception.ExamErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class JoossamengAiReportController {

    private static final String ALLOWED_TENANT = "joossameng";
    private static final String AI_REPORTS_BATCH_SCOPE = "POST /v1/exams/{examId}/reports/ai-batch";

    private final JoossamengAiReportClient client;
    private final IdempotencyService idempotencyService;

    @PostMapping("/exams/{examId}/reports/ai-batch")
    public ResponseEntity<JobAcceptedResponse> createAiBatchReport(
            @PathVariable Long examId,
            @RequestBody ReportBatchRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        String tenantSlug = requireJoossamengTenant();
        if (idempotencyKey != null) {
            return idempotencyService.executeOnce(
                    idempotencyKey,
                    AI_REPORTS_BATCH_SCOPE,
                    java.util.Map.of("examId", examId, "request", request),
                    () -> acceptedAiReportJob(examId, client.createBatch(examId, request, tenantSlug).jobId())
            );
        }
        AiReportCreateResponse response = client.createBatch(examId, request, tenantSlug);
        return acceptedAiReportJob(examId, response.jobId());
    }

    private ResponseEntity<JobAcceptedResponse> acceptedAiReportJob(Long examId, Long jobId) {
        String statusUrl = "/v1/exams/" + examId + "/reports/ai-jobs/" + jobId;
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(URI.create(statusUrl))
                .body(new JobAcceptedResponse(jobId, statusUrl));
    }

    @GetMapping("/exams/{examId}/reports/ai-jobs/{jobId}")
    public ResponseEntity<AiReportJobStatusResponse> getAiReportJobStatus(
            @PathVariable Long examId,
            @PathVariable Long jobId
    ) {
        String tenantSlug = requireJoossamengTenant();
        return ResponseEntity.ok(client.getStatus(jobId, tenantSlug));
    }

    @GetMapping(value = "/exams/{examId}/reports/ai-jobs/{jobId}/download", produces = "application/zip")
    public ResponseEntity<byte[]> downloadAiReportZip(
            @PathVariable Long examId,
            @PathVariable Long jobId
    ) {
        String tenantSlug = requireJoossamengTenant();
        byte[] zipBytes = client.downloadZip(jobId, tenantSlug);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/zip"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                        .filename("ai_reports_exam_" + examId + "_job_" + jobId + ".zip", StandardCharsets.UTF_8)
                        .build()
                        .toString());

        return ResponseEntity.ok()
                .headers(headers)
                .body(zipBytes);
    }

    private String requireJoossamengTenant() {
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        if (!ALLOWED_TENANT.equals(tenantSlug)) {
            throw new BusinessException(ExamErrorCode.TENANT_INVALID);
        }
        return tenantSlug;
    }
}
