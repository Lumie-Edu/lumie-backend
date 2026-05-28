package com.lumie.exam.application.service;

import com.lumie.exam.application.event.ReportGenerationRequestedEvent;
import com.lumie.exam.application.dto.request.ReportCallbackRequest;
import com.lumie.exam.application.dto.response.ReportJobStatusResponse;
import com.lumie.exam.application.port.out.CallbackDeduplicationPort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ReportGenerationJob;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ReportGenerationJobRepository;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
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
        ReportGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));

        if (job.getZipFileKey() == null) {
            throw new BusinessException(ExamErrorCode.RESULT_NOT_FOUND);
        }

        return storagePort.getObject(job.getZipFileKey());
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
            ReportGenerationJob job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));

            String tenantId = TenantContextHolder.toStorageTenantId(job.getTenantSlug());
            Long examId = job.getExam().getId();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (Long studentId : job.getStudentIds()) {
                    String fileKey = "tmp/" + tenantId + "/reports/" + examId + "/" + studentId + ".jpg";
                    try {
                        byte[] jpgBytes = storagePort.getObject(fileKey);
                        zos.putNextEntry(new ZipEntry("report_" + studentId + ".jpg"));
                        zos.write(jpgBytes);
                        zos.closeEntry();
                    } catch (Exception e) {
                        log.warn("Failed to add report to ZIP: studentId={}", studentId, e);
                    }
                }
            }

            String zipKey = "tmp/" + tenantId + "/reports/" + examId + "/batch_" + jobId + ".zip";
            storagePort.putObject(zipKey, baos.toByteArray(), "application/zip");
            tx.setZipFileKey(jobId, zipKey);

            log.info("ZIP created: jobId={}, key={}", jobId, zipKey);
        } catch (Exception e) {
            log.error("Failed to create ZIP: jobId={}", jobId, e);
        }
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

        @Transactional
        public void setZipFileKey(Long jobId, String zipKey) {
            jobRepository.setZipFileKey(jobId, zipKey);
        }
    }
}
