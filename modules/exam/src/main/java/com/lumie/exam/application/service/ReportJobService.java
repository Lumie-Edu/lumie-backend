package com.lumie.exam.application.service;

import com.lumie.file.api.FileObjectKeyFactory;
import com.lumie.exam.application.event.ReportBatchGenerationRequestedEvent;
import com.lumie.exam.application.dto.request.ReportCallbackRequest;
import com.lumie.exam.application.dto.response.ReportJobStatusResponse;
import com.lumie.exam.application.port.out.CallbackDeduplicationPort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ReportGenerationItem;
import com.lumie.exam.domain.entity.ReportGenerationJob;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.vo.JobStatus;
import com.lumie.exam.domain.vo.ReportGenerationItemStatus;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ReportGenerationJobRepository;
import com.lumie.exam.domain.repository.ReportGenerationItemRepository;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.student.api.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
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
    private static final Duration ZIP_DOWNLOAD_WINDOW = Duration.ofHours(24);

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
        List<ReportGenerationItem> items = java.util.stream.IntStream.range(0, studentIds.size())
                .mapToObj(index -> ReportGenerationItem.pending(
                        savedJob.getId(), examId, index, studentIds.get(index)))
                .toList();
        tx.createItems(items);

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
                job.getZipFileKey(),
                job.getZipExpiresAt()
        );
    }

    public byte[] getZipBytes(Long jobId) {
        ReportZipDownloadState state = tx.loadZipDownloadState(jobId);

        if (isExpired(state.zipExpiresAt())) {
            throw new BusinessException(ExamErrorCode.REPORT_ZIP_EXPIRED);
        }
        if (state.zipFileKey() != null && state.zipExpiresAt() == null) {
            throw new BusinessException(ExamErrorCode.REPORT_ZIP_EXPIRED);
        }

        if (state.zipFileKey() == null) {
            if (!canCreateZip(state)) {
                throw new BusinessException(ExamErrorCode.RESULT_NOT_FOUND);
            }
            createZipFileForDownload(jobId, null);
            state = tx.loadZipDownloadState(jobId);
            if (state.zipFileKey() == null || state.zipExpiresAt() == null) {
                throw new BusinessException(ExamErrorCode.RESULT_NOT_FOUND);
            }
        }

        try {
            byte[] bytes = storagePort.getObject(state.zipFileKey());
            if (isExpired(state.zipExpiresAt())) {
                throw new BusinessException(ExamErrorCode.REPORT_ZIP_EXPIRED);
            }
            return bytes;
        } catch (RuntimeException e) {
            if (e instanceof BusinessException businessException
                    && businessException.getErrorCode() == ExamErrorCode.REPORT_ZIP_EXPIRED) {
                throw businessException;
            }
            if (!canCreateZip(state)) {
                throw e;
            }
            ReportZipDownloadState latestState = tx.loadZipDownloadState(jobId);
            if (isExpired(latestState.zipExpiresAt())) {
                throw new BusinessException(ExamErrorCode.REPORT_ZIP_EXPIRED);
            }
            log.warn("Failed to download report ZIP; retrying after rebuild: jobId={}", jobId, e);
            createZipFileForDownload(jobId, state.zipExpiresAt());
            ReportZipDownloadState refreshedState = tx.loadZipDownloadState(jobId);
            if (refreshedState.zipFileKey() == null || refreshedState.zipExpiresAt() == null) {
                throw new BusinessException(ExamErrorCode.RESULT_NOT_FOUND);
            }
            if (isExpired(refreshedState.zipExpiresAt())) {
                throw new BusinessException(ExamErrorCode.REPORT_ZIP_EXPIRED);
            }
            byte[] rebuilt = storagePort.getObject(refreshedState.zipFileKey());
            if (isExpired(refreshedState.zipExpiresAt())) {
                throw new BusinessException(ExamErrorCode.REPORT_ZIP_EXPIRED);
            }
            return rebuilt;
        }
    }

    private static boolean isExpired(Instant expiresAt) {
        return expiresAt != null && !expiresAt.isAfter(Instant.now());
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

        CallbackContext context = tx.loadCallbackContext(request);
        if (!context.open()) {
            log.warn("Late report callback skipped for terminal job: jobId={}, reportIndex={}",
                    request.jobId(), request.reportIndex());
            return;
        }

        if (!context.itemized()) {
            processLegacyCallback(request);
            return;
        }

        if (context.itemStatus() != ReportGenerationItemStatus.PENDING) {
            boolean callbackSuccess = request.success();
            boolean recordedSuccess = context.itemStatus() == ReportGenerationItemStatus.SUCCESS;
            if (callbackSuccess != recordedSuccess) {
                log.warn("Conflicting report callback ignored after terminal item state: "
                                + "jobId={}, reportIndex={}, recordedStatus={}, callbackSuccess={}",
                        request.jobId(), request.reportIndex(), context.itemStatus(), callbackSuccess);
            } else {
                log.info("Duplicate report callback skipped by item state: jobId={}, reportIndex={}, status={}",
                        request.jobId(), request.reportIndex(), context.itemStatus());
            }
            return;
        }

        CallbackOutcome outcome = prepareItemizedOutcome(request);
        CallbackPersistenceResult result = tx.completeItem(request, outcome);
        if (!result.transitioned()) {
            log.info("Concurrent report callback already transitioned item: jobId={}, reportIndex={}",
                    request.jobId(), request.reportIndex());
            return;
        }
        if (result.finalized()) {
            log.info("Report job reached terminal state: jobId={}", request.jobId());
            createZipFile(request.jobId());
        }
    }

    private void processLegacyCallback(ReportCallbackRequest request) {

        String dedupKey = "report:cb:" + request.jobId() + ":" + request.reportIndex();
        if (!callbackDeduplication.markIfAbsent(dedupKey)) {
            log.warn("Duplicate report callback skipped: jobId={}, reportIndex={}",
                    request.jobId(), request.reportIndex());
            return;
        }

        try {
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
        } catch (RuntimeException processingFailure) {
            // Keep the broker retryable when any processing step fails. The SETNX
            // reservation is only a dedup marker after successful DB processing.
            try {
                callbackDeduplication.forget(dedupKey);
            } catch (RuntimeException cleanupFailure) {
                processingFailure.addSuppressed(cleanupFailure);
            }
            throw processingFailure;
        }
    }

    private CallbackOutcome prepareItemizedOutcome(ReportCallbackRequest request) {
        if (!request.success()) {
            return CallbackOutcome.failed(boundError(request.error(), "REPORT_GENERATION_FAILED"));
        }

        String tenantId = TenantContextHolder.toStorageTenantId(request.tenantSlug());
        String expectedFileKey = buildReportFileKey(tenantId, request.examId(), request.studentId());
        if (request.fileKey() != null && !request.fileKey().isBlank()) {
            if (!expectedFileKey.equals(request.fileKey())) {
                return CallbackOutcome.failed("UNEXPECTED_FILE_KEY");
            }
            // Storage read errors are transient delivery failures. Only readable,
            // non-PDF content is a terminal report failure.
            byte[] storedBytes = storagePort.getObject(expectedFileKey);
            if (!ReportFileFormat.isPdf(storedBytes)) {
                return CallbackOutcome.failed("INVALID_REPORT_FILE");
            }
            return CallbackOutcome.succeeded(expectedFileKey);
        }

        byte[] reportBytes = decodeReportBytes(request.reportBytes());
        if (!ReportFileFormat.isPdf(reportBytes)) {
            return CallbackOutcome.failed("INVALID_REPORT_FILE");
        }

        // The storage write stays outside the DB transaction. Retrying writes
        // the same deterministic key; the DB state transition gates counting.
        storagePort.putObject(expectedFileKey, reportBytes, ReportFileFormat.PDF_CONTENT_TYPE);
        return CallbackOutcome.succeeded(expectedFileKey);
    }

    private static String boundError(String error, String fallback) {
        String message = error == null || error.isBlank() ? fallback : error;
        return message.substring(0, Math.min(1000, message.length()));
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
            createZipFileOrThrow(jobId, null);
        } catch (Exception e) {
            log.error("Failed to create ZIP: jobId={}", jobId, e);
        }
    }

    private void createZipFileForDownload(Long jobId, Instant existingExpiry) {
        try {
            createZipFileOrThrow(jobId, existingExpiry);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create report ZIP", e);
        }
    }

    private boolean canCreateZip(ReportZipDownloadState state) {
        return state.status() == JobStatus.COMPLETED && state.successCount() > 0;
    }

    private void createZipFileOrThrow(Long jobId, Instant existingExpiry) throws Exception {
        ReportZipManifest manifest = tx.loadZipManifest(jobId);
        String tenantId = TenantContextHolder.toStorageTenantId(manifest.tenantSlug());
        Map<String, Integer> entryNameCounts = new HashMap<>();
        int addedReports = 0;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (ReportZipItem report : manifest.reports()) {
                if (isExpired(existingExpiry)) {
                    throw new BusinessException(ExamErrorCode.REPORT_ZIP_EXPIRED);
                }
                try {
                    StoredReport storedReport = getStoredReport(
                            tenantId, manifest.examId(), report.studentId(), report.fileKey(),
                            report.allowLegacyJpg(), existingExpiry);
                    if (isExpired(existingExpiry)) {
                        throw new BusinessException(ExamErrorCode.REPORT_ZIP_EXPIRED);
                    }
                    String studentName = manifest.studentNamesById()
                            .getOrDefault(report.studentId(), "학생 " + report.studentId());
                    String entryName = uniqueFilename(
                            buildReportFilename(studentName, manifest.examName(), storedReport.extension()),
                            entryNameCounts);
                    zos.putNextEntry(new ZipEntry(entryName));
                    zos.write(storedReport.bytes());
                    zos.closeEntry();
                    addedReports++;
                } catch (Exception e) {
                    if (e instanceof BusinessException businessException
                            && businessException.getErrorCode() == ExamErrorCode.REPORT_ZIP_EXPIRED) {
                        throw businessException;
                    }
                    log.warn("Failed to add report to ZIP: studentId={}", report.studentId(), e);
                }
            }
        }

        if (isExpired(existingExpiry)) {
            throw new BusinessException(ExamErrorCode.REPORT_ZIP_EXPIRED);
        }
        if (addedReports != manifest.successCount()) {
            throw new IllegalStateException("Cannot store an incomplete report ZIP: expected "
                    + manifest.successCount() + " entries, found " + addedReports);
        }

        String zipKey = FileObjectKeyFactory.reportArchive(tenantId, manifest.examId(), jobId);
        storagePort.putObject(zipKey, baos.toByteArray(), "application/zip");
        if (existingExpiry == null) {
            Instant expiresAt = Instant.now().plus(ZIP_DOWNLOAD_WINDOW);
            tx.setZipFileKey(jobId, zipKey, expiresAt);
        }

        log.info("ZIP created: jobId={}, key={}", jobId, zipKey);
    }

    private StoredReport getStoredReport(
            String tenantId, Long examId, Long studentId, String fileKey,
            boolean allowLegacyJpg, Instant existingExpiry) {
        if (isExpired(existingExpiry)) {
            throw new BusinessException(ExamErrorCode.REPORT_ZIP_EXPIRED);
        }
        try {
            byte[] bytes = storagePort.getObject(fileKey);
            if (isExpired(existingExpiry)) {
                throw new BusinessException(ExamErrorCode.REPORT_ZIP_EXPIRED);
            }
            return new StoredReport(bytes, ReportFileFormat.PDF_EXTENSION);
        } catch (RuntimeException pdfFailure) {
            if (isExpired(existingExpiry)) {
                throw new BusinessException(ExamErrorCode.REPORT_ZIP_EXPIRED);
            }
            if (!allowLegacyJpg) {
                throw pdfFailure;
            }
            String legacyFileKey = buildLegacyReportFileKey(tenantId, examId, studentId);
            try {
                byte[] bytes = storagePort.getObject(legacyFileKey);
                if (isExpired(existingExpiry)) {
                    throw new BusinessException(ExamErrorCode.REPORT_ZIP_EXPIRED);
                }
                return new StoredReport(bytes, ReportFileFormat.LEGACY_JPG_EXTENSION);
            } catch (RuntimeException legacyFailure) {
                if (isExpired(existingExpiry)) {
                    throw new BusinessException(ExamErrorCode.REPORT_ZIP_EXPIRED);
                }
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
        return com.lumie.file.api.FileObjectKeyFactory.temporaryReport(tenantId, examId, studentId);
    }

    private static String buildLegacyReportFileKey(String tenantId, Long examId, Long studentId) {
        return FileObjectKeyFactory.legacyReport(tenantId, examId, studentId);
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

    private record ReportZipManifest(String tenantSlug, Long examId, String examName, int successCount,
                                     List<ReportZipItem> reports, Map<Long, String> studentNamesById) {
    }

    private record ReportZipItem(Long studentId, String fileKey, boolean allowLegacyJpg) {
    }

    private record ReportZipDownloadState(JobStatus status, int successCount, String zipFileKey, Instant zipExpiresAt) {
    }

    private record StoredReport(byte[] bytes, String extension) {
    }

    record CallbackContext(boolean itemized, boolean open, ReportGenerationItemStatus itemStatus) {
    }

    record CallbackOutcome(boolean success, String fileKey, String errorMessage) {
        static CallbackOutcome succeeded(String fileKey) {
            return new CallbackOutcome(true, fileKey, null);
        }

        static CallbackOutcome failed(String errorMessage) {
            return new CallbackOutcome(false, null, errorMessage);
        }
    }

    record CallbackPersistenceResult(boolean transitioned, boolean finalized) {
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
        private final ReportGenerationItemRepository itemRepository;
        private final StudentService studentService;

        @Transactional
        public void createItems(List<ReportGenerationItem> items) {
            itemRepository.saveAll(items);
        }

        @Transactional(readOnly = true)
        public CallbackContext loadCallbackContext(ReportCallbackRequest request) {
            if (request.jobId() == null || request.examId() == null || request.studentId() == null
                    || request.tenantSlug() == null || request.tenantSlug().isBlank()
                    || request.totalReports() < 1 || request.reportIndex() < 0
                    || request.reportIndex() >= request.totalReports()) {
                throw invalidCallback("Missing or invalid report callback identity");
            }

            ReportGenerationJob job = jobRepository.findById(request.jobId())
                    .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));
            if (!request.tenantSlug().equals(job.getTenantSlug())
                    || !request.examId().equals(job.getExam().getId())
                    || request.totalReports() != job.getTotalReports()
                    || job.getStudentIds() == null
                    || request.reportIndex() >= job.getStudentIds().size()
                    || !request.studentId().equals(job.getStudentIds().get(request.reportIndex()))) {
                throw invalidCallback("Report callback does not match its job identity");
            }

            boolean open = job.getStatus() == JobStatus.PENDING || job.getStatus() == JobStatus.PROCESSING;
            if (!job.isCallbacksItemized()) {
                return new CallbackContext(false, open, null);
            }

            ReportGenerationItem item = itemRepository
                    .findByJobIdAndReportIndex(request.jobId(), request.reportIndex())
                    .orElseThrow(() -> invalidCallback("Report item is missing"));
            if (!request.examId().equals(item.getExamId())
                    || !request.studentId().equals(item.getStudentId())) {
                throw invalidCallback("Report callback does not match its report item");
            }
            return new CallbackContext(true, open, item.getStatus());
        }

        @Transactional
        public CallbackPersistenceResult completeItem(ReportCallbackRequest request, CallbackOutcome outcome) {
            ReportGenerationJob job = jobRepository.findByIdForUpdate(request.jobId())
                    .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));
            if (!job.isCallbacksItemized() || !isOpen(job.getStatus())) {
                return new CallbackPersistenceResult(false, false);
            }
            validateCallbackMatchesJob(request, job);

            ReportGenerationItem item = itemRepository
                    .findByJobIdAndReportIndexForUpdate(request.jobId(), request.reportIndex())
                    .orElseThrow(() -> invalidCallback("Report item is missing"));
            if (!request.examId().equals(item.getExamId())
                    || !request.studentId().equals(item.getStudentId())) {
                throw invalidCallback("Report callback does not match its report item");
            }

            boolean transitioned = outcome.success()
                    ? item.completeSuccessfully(outcome.fileKey())
                    : item.completeWithFailure(outcome.errorMessage());
            if (!transitioned) {
                return new CallbackPersistenceResult(false, false);
            }

            jobRepository.startProcessingIfPending(request.jobId());
            if (outcome.success()) {
                jobRepository.incrementSuccess(request.jobId());
            } else {
                jobRepository.incrementFail(request.jobId());
            }
            boolean finalized = jobRepository.tryFinalize(request.jobId(), job.getTotalReports());
            return new CallbackPersistenceResult(true, finalized);
        }

        private static void validateCallbackMatchesJob(ReportCallbackRequest request, ReportGenerationJob job) {
            if (!request.tenantSlug().equals(job.getTenantSlug())
                    || !request.examId().equals(job.getExam().getId())
                    || request.totalReports() != job.getTotalReports()
                    || job.getStudentIds() == null
                    || request.reportIndex() >= job.getStudentIds().size()
                    || !request.studentId().equals(job.getStudentIds().get(request.reportIndex()))) {
                throw invalidCallback("Report callback does not match its job identity");
            }
        }

        private static boolean isOpen(JobStatus status) {
            return status == JobStatus.PENDING || status == JobStatus.PROCESSING;
        }

        private static BusinessException invalidCallback(String message) {
            return new BusinessException(ExamErrorCode.INVALID_EXAM_DATA, message);
        }

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
            Long examId = job.getExam().getId();
            String storageTenantId = TenantContextHolder.toStorageTenantId(tenantSlug);
            List<ReportZipItem> reports = job.isCallbacksItemized()
                    ? itemRepository.findByJobIdOrderByReportIndexAsc(jobId).stream()
                            .filter(item -> item.getStatus() == ReportGenerationItemStatus.SUCCESS)
                            .map(item -> new ReportZipItem(item.getStudentId(), item.getFileKey(), false))
                            .toList()
                    : job.getStudentIds().stream()
                            .map(studentId -> new ReportZipItem(
                                    studentId, buildReportFileKey(storageTenantId, examId, studentId), true))
                            .toList();
            List<Long> reportStudentIds = reports.stream().map(ReportZipItem::studentId).distinct().toList();
            Map<Long, String> studentNamesById = studentService.getStudentsByIds(
                            tenantSlug,
                            reportStudentIds
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
                    examId,
                    job.getExam().getName(),
                    job.getSuccessCount(),
                    reports,
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
                    job.getZipFileKey(),
                    job.getZipExpiresAt()
            );
        }

        @Transactional
        public void setZipFileKey(Long jobId, String zipKey, Instant expiresAt) {
            jobRepository.setZipFileKey(jobId, zipKey, expiresAt);
        }
    }
}
