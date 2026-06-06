package com.lumie.exam.application.service;

import com.lumie.exam.application.event.ReportGenerationRequestedEvent;
import com.lumie.exam.application.dto.request.ReportCallbackRequest;
import com.lumie.exam.application.dto.response.ReportJobStatusResponse;
import com.lumie.exam.application.port.out.CallbackDeduplicationPort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ReportGenerationJob;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.vo.JobStatus;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ReportGenerationJobRepository;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.student.api.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportJobService {

    private final ExamRepository examRepository;
    private final ReportGenerationJobRepository jobRepository;
    private final OmrStoragePort storagePort;
    private final CallbackDeduplicationPort callbackDeduplication;
    private final ApplicationEventPublisher eventPublisher;
    private final StudentService studentService;
    private final Tx tx;

    @Transactional
    public Long createBatchJob(Long examId, List<Long> studentIds) {
        log.info("Creating batch report job: examId={}, students={}", examId, studentIds.size());

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));

        String tenantSlug = TenantContextHolder.getRequiredTenant();

        ReportGenerationJob job = ReportGenerationJob.create(exam, tenantSlug, studentIds);
        ReportGenerationJob savedJob = jobRepository.save(job);

        // Outbox: each event is persisted to event_publication in THIS transaction,
        // atomically with the job row, then forwarded to RabbitMQ after commit by
        // GradingRequestForwarder. Closes the commit→broker gap.
        for (int i = 0; i < studentIds.size(); i++) {
            eventPublisher.publishEvent(new ReportGenerationRequestedEvent(
                    savedJob.getId(), examId, studentIds.get(i),
                    tenantSlug, i, studentIds.size()));
        }
        log.info("Batch report job queued: jobId={}, reports={}",
                savedJob.getId(), studentIds.size());

        return savedJob.getId();
    }

    @Transactional(readOnly = true)
    public ReportJobStatusResponse getJobStatus(Long jobId) {
        ReportGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));

        return new ReportJobStatusResponse(
                job.getId(),
                job.getStatus().name(),
                job.getTotalReports(),
                job.getProcessedReports(),
                job.getSuccessCount(),
                job.getFailCount(),
                job.getZipFileKey()
        );
    }

    public byte[] getZipBytes(Long jobId) {
        ReportZipDownloadState state = tx.loadZipDownloadState(jobId);

        if (state.zipFileKey() == null) {
            if (!canCreateZip(state)) {
                throw new BusinessException(ExamErrorCode.RESULT_NOT_FOUND);
            }
            createZipFileForDownload(jobId);
            state = tx.loadZipDownloadState(jobId);
            if (state.zipFileKey() == null) {
                throw new BusinessException(ExamErrorCode.RESULT_NOT_FOUND);
            }
        }

        try {
            return storagePort.getObject(state.zipFileKey());
        } catch (RuntimeException e) {
            if (!canCreateZip(state)) {
                throw e;
            }
            log.warn("Failed to download report ZIP; retrying after rebuild: jobId={}", jobId, e);
            createZipFileForDownload(jobId);
            ReportZipDownloadState refreshedState = tx.loadZipDownloadState(jobId);
            if (refreshedState.zipFileKey() == null) {
                throw new BusinessException(ExamErrorCode.RESULT_NOT_FOUND);
            }
            return storagePort.getObject(refreshedState.zipFileKey());
        }
    }

    @Transactional(readOnly = true)
    public String getSingleReportFilename(Long studentId, Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        String studentName = studentService.getStudent(tenantSlug, studentId)
                .map(StudentService.StudentData::studentName)
                .orElse("학생 " + studentId);
        return buildReportImageFilename(studentName, exam.getName());
    }

    public String getZipFilename(Long jobId) {
        ReportZipManifest manifest = tx.loadZipManifest(jobId);
        return buildZipFilename(manifest.examName());
    }

    public void processCallback(ReportCallbackRequest request) {
        log.info("Processing report callback: jobId={}, studentId={}, success={}",
                request.jobId(), request.studentId(), request.success());

        String dedupKey = "report:cb:" + request.jobId() + ":" + request.reportIndex();
        if (!callbackDeduplication.markIfAbsent(dedupKey)) {
            log.warn("Duplicate report callback skipped: jobId={}, reportIndex={}",
                    request.jobId(), request.reportIndex());
            return;
        }

        tx.markProcessing(request.jobId());

        if (request.success()) {
            byte[] jpgBytes = Base64.getDecoder().decode(request.reportBytes());
            String tenantId = TenantContextHolder.toStorageTenantId(request.tenantSlug());
            String fileKey = "tmp/" + tenantId + "/reports/" + request.examId() + "/" + request.studentId() + ".jpg";
            // storage call outside any @Transactional boundary
            storagePort.putObject(fileKey, jpgBytes, "image/jpeg");
            tx.recordSuccess(request.jobId());
        } else {
            log.warn("Report generation failed: jobId={}, studentId={}, error={}",
                    request.jobId(), request.studentId(), request.error());
            tx.recordFail(request.jobId());
        }

        if (tx.finalizeIfDone(request.jobId(), request.totalReports())) {
            log.info("Report job reached terminal state: jobId={}", request.jobId());
            createZipFile(request.jobId());
        }
    }

    private void createZipFile(Long jobId) {
        try {
            createZipFileOrThrow(jobId);
        } catch (Exception e) {
            log.error("Failed to create ZIP: jobId={}", jobId, e);
        }
    }

    private void createZipFileForDownload(Long jobId) {
        try {
            createZipFileOrThrow(jobId);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create report ZIP", e);
        }
    }

    private boolean canCreateZip(ReportZipDownloadState state) {
        return state.status() == JobStatus.COMPLETED && state.successCount() > 0;
    }

    private void createZipFileOrThrow(Long jobId) throws Exception {
        ReportZipManifest manifest = tx.loadZipManifest(jobId);
        String tenantId = TenantContextHolder.toStorageTenantId(manifest.tenantSlug());
        Map<String, Integer> entryNameCounts = new HashMap<>();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Long studentId : manifest.studentIds()) {
                String fileKey = "tmp/" + tenantId + "/reports/" + manifest.examId() + "/" + studentId + ".jpg";
                try {
                    byte[] jpgBytes = storagePort.getObject(fileKey);
                    String studentName = manifest.studentNamesById()
                            .getOrDefault(studentId, "학생 " + studentId);
                    String entryName = uniqueFilename(
                            buildReportImageFilename(studentName, manifest.examName()),
                            entryNameCounts);
                    zos.putNextEntry(new ZipEntry(entryName));
                    zos.write(jpgBytes);
                    zos.closeEntry();
                } catch (Exception e) {
                    log.warn("Failed to add report to ZIP: studentId={}", studentId, e);
                }
            }
        }

        String zipKey = "tmp/" + tenantId + "/reports/" + manifest.examId() + "/batch_" + jobId + ".zip";
        storagePort.putObject(zipKey, baos.toByteArray(), "application/zip");
        tx.setZipFileKey(jobId, zipKey);

        log.info("ZIP created: jobId={}, key={}", jobId, zipKey);
    }

    private static String buildReportImageFilename(String studentName, String examName) {
        return sanitizeFilenamePart(studentName) + "_" + sanitizeFilenamePart(examName) + ".jpg";
    }

    private static String buildZipFilename(String examName) {
        return sanitizeFilenamePart(examName) + "_리포트.zip";
    }

    private static String uniqueFilename(String filename, Map<String, Integer> counts) {
        int count = counts.getOrDefault(filename, 0) + 1;
        counts.put(filename, count);
        if (count == 1) {
            return filename;
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0) {
            return filename + "_" + count;
        }
        return filename.substring(0, dotIndex) + "_" + count + filename.substring(dotIndex);
    }

    private static String sanitizeFilenamePart(String value) {
        String sanitized = value == null ? "" : value.trim();
        sanitized = sanitized
                .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        return sanitized.isBlank() ? "리포트" : sanitized;
    }

    private record ReportZipManifest(String tenantSlug, Long examId, String examName,
                                     List<Long> studentIds, Map<Long, String> studentNamesById) {
    }

    private record ReportZipDownloadState(JobStatus status, int successCount, String zipFileKey) {
    }

    /**
     * Tx-wrapped DB ops. Mirrors {@link OmrGradingJobProcessor.Tx} — the @Modifying
     * native queries each need an active transaction; wrapping at the method
     * boundary keeps the storage/HTTP calls in {@code processCallback} outside
     * any tx (HARD_RULES "wrap external HTTP outside @Transactional").
     */
    @Component
    @RequiredArgsConstructor
    static class Tx {
        private final ReportGenerationJobRepository jobRepository;
        private final StudentService studentService;

        @Transactional
        public void markProcessing(Long jobId) {
            jobRepository.startProcessingIfPending(jobId);
        }

        @Transactional
        public void recordSuccess(Long jobId) {
            jobRepository.incrementSuccess(jobId);
        }

        @Transactional
        public void recordFail(Long jobId) {
            jobRepository.incrementFail(jobId);
        }

        @Transactional
        public boolean finalizeIfDone(Long jobId, int totalReports) {
            return jobRepository.tryFinalize(jobId, totalReports);
        }

        @Transactional(readOnly = true)
        public ReportZipManifest loadZipManifest(Long jobId) {
            ReportGenerationJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));
            String tenantSlug = job.getTenantSlug();
            Map<Long, String> studentNamesById = studentService.getStudentsByIds(
                            tenantSlug,
                            List.copyOf(job.getStudentIds())
                    )
                    .students()
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(
                            StudentService.StudentData::id,
                            StudentService.StudentData::studentName,
                            (first, ignored) -> first
                    ));
            return new ReportZipManifest(
                    tenantSlug,
                    job.getExam().getId(),
                    job.getExam().getName(),
                    List.copyOf(job.getStudentIds()),
                    studentNamesById
            );
        }

        @Transactional(readOnly = true)
        public ReportZipDownloadState loadZipDownloadState(Long jobId) {
            ReportGenerationJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));
            return new ReportZipDownloadState(
                    job.getStatus(),
                    job.getSuccessCount(),
                    job.getZipFileKey()
            );
        }

        @Transactional
        public void setZipFileKey(Long jobId, String zipKey) {
            jobRepository.setZipFileKey(jobId, zipKey);
        }
    }
}
