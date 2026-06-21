package com.lumie.exam.application.service;

import com.lumie.exam.application.event.ReportBatchGenerationRequestedEvent;
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

    private static final int REPORT_BATCH_CHUNK_SIZE = 30;

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
        for (int startIndex = 0; startIndex < studentIds.size(); startIndex += REPORT_BATCH_CHUNK_SIZE) {
            int endIndex = Math.min(startIndex + REPORT_BATCH_CHUNK_SIZE, studentIds.size());
            eventPublisher.publishEvent(new ReportBatchGenerationRequestedEvent(
                    savedJob.getId(), examId,
                    List.copyOf(studentIds.subList(startIndex, endIndex)),
                    tenantSlug, startIndex, studentIds.size()));
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
        return buildReportFilename(studentName, exam.getName());
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

        if (!tx.isOpenJob(request.jobId())) {
            log.warn("Late report callback skipped for terminal or missing job: jobId={}, reportIndex={}",
                    request.jobId(), request.reportIndex());
            return;
        }

        tx.markProcessing(request.jobId());

        if (request.success()) {
            processSuccessfulCallback(request);
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

    private void processSuccessfulCallback(ReportCallbackRequest request) {
        String tenantId = TenantContextHolder.toStorageTenantId(request.tenantSlug());
        String expectedFileKey = buildReportFileKey(tenantId, request.examId(), request.studentId());
        if (request.fileKey() != null && !request.fileKey().isBlank()) {
            if (!expectedFileKey.equals(request.fileKey())) {
                log.warn("Report callback returned unexpected fileKey: jobId={}, studentId={}, fileKey={}",
                        request.jobId(), request.studentId(), request.fileKey());
                tx.recordFail(request.jobId());
                return;
            }
            if (!isReadablePdf(expectedFileKey)) {
                log.warn("Report callback fileKey is not a readable PDF: jobId={}, studentId={}, fileKey={}",
                        request.jobId(), request.studentId(), request.fileKey());
                tx.recordFail(request.jobId());
                return;
            }
            tx.recordSuccess(request.jobId());
            return;
        }

        byte[] reportBytes = decodeReportBytes(request.reportBytes());
        if (!ReportFileFormat.isPdf(reportBytes)) {
            log.warn("Report callback returned non-PDF bytes: jobId={}, studentId={}",
                    request.jobId(), request.studentId());
            tx.recordFail(request.jobId());
            return;
        }

        // storage call outside any @Transactional boundary
        storagePort.putObject(expectedFileKey, reportBytes, ReportFileFormat.PDF_CONTENT_TYPE);
        tx.recordSuccess(request.jobId());
    }

    private boolean isReadablePdf(String fileKey) {
        try {
            return ReportFileFormat.isPdf(storagePort.getObject(fileKey));
        } catch (RuntimeException e) {
            log.warn("Failed to read report fileKey before callback success: fileKey={}", fileKey, e);
            return false;
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
                try {
                    StoredReport storedReport = getStoredReport(tenantId, manifest.examId(), studentId);
                    String studentName = manifest.studentNamesById()
                            .getOrDefault(studentId, "학생 " + studentId);
                    String entryName = uniqueFilename(
                            buildReportFilename(studentName, manifest.examName(), storedReport.extension()),
                            entryNameCounts);
                    zos.putNextEntry(new ZipEntry(entryName));
                    zos.write(storedReport.bytes());
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

    private StoredReport getStoredReport(String tenantId, Long examId, Long studentId) {
        String fileKey = buildReportFileKey(tenantId, examId, studentId);
        try {
            return new StoredReport(storagePort.getObject(fileKey), ReportFileFormat.PDF_EXTENSION);
        } catch (RuntimeException pdfFailure) {
            String legacyFileKey = buildLegacyReportFileKey(tenantId, examId, studentId);
            try {
                return new StoredReport(storagePort.getObject(legacyFileKey), ReportFileFormat.LEGACY_JPG_EXTENSION);
            } catch (RuntimeException legacyFailure) {
                pdfFailure.addSuppressed(legacyFailure);
                throw pdfFailure;
            }
        }
    }

    private static byte[] decodeReportBytes(String reportBytes) {
        if (reportBytes == null) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(reportBytes);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String buildReportFileKey(String tenantId, Long examId, Long studentId) {
        return "tmp/" + tenantId + "/reports/" + examId + "/" + studentId + ReportFileFormat.PDF_EXTENSION;
    }

    private static String buildLegacyReportFileKey(String tenantId, Long examId, Long studentId) {
        return "tmp/" + tenantId + "/reports/" + examId + "/" + studentId + ReportFileFormat.LEGACY_JPG_EXTENSION;
    }

    private static String buildReportFilename(String studentName, String examName) {
        return buildReportFilename(studentName, examName, ReportFileFormat.PDF_EXTENSION);
    }

    private static String buildReportFilename(String studentName, String examName, String extension) {
        return sanitizeFilenamePart(studentName) + "_" + sanitizeFilenamePart(examName) + extension;
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

    private record StoredReport(byte[] bytes, String extension) {
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

        @Transactional(readOnly = true)
        public boolean isOpenJob(Long jobId) {
            return jobRepository.existsByIdAndStatusIn(
                    jobId,
                    List.of(JobStatus.PENDING, JobStatus.PROCESSING)
            );
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
