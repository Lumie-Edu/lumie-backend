package com.lumie.exam.adapter.in.web;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.domain.entity.OmrGradingJob;
import com.lumie.exam.domain.entity.ReportGenerationJob;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.exam.domain.repository.ReportGenerationJobRepository;
import com.lumie.exam.domain.vo.JobStatus;
import com.lumie.file.api.FileObjectService;
import com.lumie.file.api.FileObjectKeyFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/** HMAC-protected, job-scoped grants used by Python workers. */
@RestController
@RequestMapping("/internal/files/worker")
@RequiredArgsConstructor
@Slf4j
public class InternalWorkerFileController {
    private static final int URL_EXPIRY_SECONDS = 15 * 60;
    private final OmrGradingJobRepository omrJobs;
    private final ReportGenerationJobRepository reportJobs;
    private final FileObjectService fileObjectService;

    @GetMapping("/omr-jobs/{jobId}/items/{imageIndex}/download-url")
    @Transactional(readOnly = true)
    public ResponseEntity<DownloadGrant> omrDownload(@PathVariable Long jobId, @PathVariable int imageIndex) {
        OmrGradingJob job = omrJobs.findById(jobId).orElseThrow(() -> new BusinessException(ExamErrorCode.INVALID_EXAM_DATA));
        requireOpenJob(job.getTenantSlug(), job.getStatus());
        if (imageIndex < 0 || imageIndex >= job.getImageKeys().size()) throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA);
        return ResponseEntity.ok(new DownloadGrant(fileObjectService.presignedInternalDownloadUrl(job.getImageKeys().get(imageIndex), URL_EXPIRY_SECONDS), expiresAt()));
    }

    @PostMapping("/report-jobs/{jobId}/items/{reportIndex}/upload-url")
    @Transactional(readOnly = true)
    public ResponseEntity<UploadGrant> reportUpload(@PathVariable Long jobId, @PathVariable int reportIndex,
                                                     @Valid @RequestBody ReportUploadRequest request) {
        ReportGenerationJob job = reportJobs.findById(jobId).orElseThrow(() -> new BusinessException(ExamErrorCode.INVALID_EXAM_DATA));
        requireOpenJob(job.getTenantSlug(), job.getStatus());
        if (reportIndex < 0 || reportIndex >= job.getStudentIds().size() || !job.getStudentIds().get(reportIndex).equals(request.studentId())
                || !job.getExam().getId().equals(request.examId()) || !"application/pdf".equals(request.contentType())) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA);
        }
        String tenantId = TenantContextHolder.toStorageTenantId(job.getTenantSlug());
        String key = FileObjectKeyFactory.temporaryReport(tenantId, request.examId(), request.studentId());
        return ResponseEntity.ok(new UploadGrant(fileObjectService.presignedInternalUploadUrl(key, request.contentType(), URL_EXPIRY_SECONDS), java.util.Map.of(), key, expiresAt()));
    }

    private static void requireOpenJob(String tenantSlug, JobStatus status) {
        String requestTenant = TenantContextHolder.getRequiredTenant();
        if (!tenantSlug.equals(requestTenant) || (status != JobStatus.PENDING && status != JobStatus.PROCESSING)) {
            log.warn("Rejected worker file grant: jobTenant={}, requestTenant={}, status={}", tenantSlug, requestTenant, status);
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA);
        }
    }
    private static Instant expiresAt() { return Instant.now().plusSeconds(URL_EXPIRY_SECONDS); }
    public record DownloadGrant(String downloadUrl, Instant expiresAt) {}
    public record UploadGrant(String uploadUrl, java.util.Map<String, String> requiredHeaders, String fileKey, Instant expiresAt) {}
    public record ReportUploadRequest(@NotNull Long examId, @NotNull Long studentId, @NotBlank String contentType, @NotNull @Positive Long contentLength) {}
}
