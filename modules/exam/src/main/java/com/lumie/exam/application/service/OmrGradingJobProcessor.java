package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.request.OmrGradingCallbackRequest;
import com.lumie.exam.application.port.out.CallbackDeduplicationPort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.domain.entity.OmrGradingJob;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.exam.domain.vo.JobStatus;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class OmrGradingJobProcessor {

    private static final String DEDUP_KEY_PREFIX = "omr:cb:";
    private static final int MAX_OPTIMISTIC_LOCK_ATTEMPTS = 3;
    private static final long OPTIMISTIC_LOCK_BACKOFF_MILLIS = 50;

    private final OmrStoragePort storagePort;
    private final CallbackDeduplicationPort callbackDeduplication;
    private final OmrGradingJobRepository jobRepository;
    private final Tx tx;

    public void processGradingCallback(OmrGradingCallbackRequest request) {
        log.info("Processing grading callback: jobId={}, imageIndex={}/{}, success={}",
                request.jobId(), request.imageIndex() + 1, request.totalImages(), request.success());

        String dedupKey = DEDUP_KEY_PREFIX + request.jobId() + ":" + request.imageIndex();
        for (int attempt = 1; attempt <= MAX_OPTIMISTIC_LOCK_ATTEMPTS; attempt++) {
            if (!callbackDeduplication.markIfAbsent(dedupKey)) {
                log.warn("Duplicate OMR callback skips counting and retries finalization: jobId={}, imageIndex={}",
                        request.jobId(), request.imageIndex());
                finalizeAndCleanupIfDone(request);
                return;
            }

            boolean counted;
            try {
                counted = recordCallbackBeforeFinalize(request);
            } catch (OptimisticLockingFailureException e) {
                forgetDedupAfterUncommittedFailure(dedupKey, request);
                if (attempt == MAX_OPTIMISTIC_LOCK_ATTEMPTS) {
                    throw e;
                }
                log.warn("Retrying OMR callback after optimistic lock failure: jobId={}, imageIndex={}, attempt={}/{}",
                        request.jobId(), request.imageIndex(), attempt + 1, MAX_OPTIMISTIC_LOCK_ATTEMPTS);
                backoff(attempt);
                continue;
            } catch (RuntimeException e) {
                forgetDedupAfterUncommittedFailure(dedupKey, request);
                throw e;
            }

            if (counted) {
                finalizeAndCleanupIfDone(request);
            }
            return;
        }
    }

    private void finalizeAndCleanupIfDone(OmrGradingCallbackRequest request) {
        if (tx.finalizeIfDone(request.jobId(), request.totalImages())) {
            log.info("OMR grading job reached terminal state: jobId={}", request.jobId());
            cleanupImages(request.jobId());
        }
    }

    private boolean recordCallbackBeforeFinalize(OmrGradingCallbackRequest request) {
        if (!tx.isOpenJob(request.jobId())) {
            log.warn("Late OMR callback skipped for terminal or missing job: jobId={}, imageIndex={}",
                    request.jobId(), request.imageIndex());
            return false;
        }

        tx.markProcessing(request.jobId());

        if (request.success()) {
            // Phase 1 (read tx): resolve the immutable submission and its unique image key.
            Tx.SuccessPrep prep = tx.prepareSuccess(request);

            // Phase 2 (no tx): copy image in MinIO. A failed copy keeps the source image for recovery.
            String permanentKey;
            try {
                permanentKey = copyImage(request, prep.permanentKey());
            } catch (Exception e) {
                log.warn("Failed to copy OMR image to permanent path: {}", prep.permanentKey(), e);
                tx.recordFail(request.jobId(), request.imageIndex(), "IMAGE_PERSISTENCE_FAILED");
                return true;
            }

            // Phase 3 (write tx): persist raw submission, then conditionally project it.
            tx.saveSuccess(request, prep, permanentKey);
        } else {
            tx.recordFail(request.jobId(), request.imageIndex(), request.error());
        }

        return true;
    }

    private void forgetDedupAfterUncommittedFailure(String dedupKey, OmrGradingCallbackRequest request) {
        try {
            callbackDeduplication.forget(dedupKey);
        } catch (RuntimeException e) {
            log.warn("Failed to release OMR callback dedup key after uncommitted failure: jobId={}, imageIndex={}",
                    request.jobId(), request.imageIndex(), e);
        }
    }

    private static void backoff(int previousAttempt) {
        long millis = OPTIMISTIC_LOCK_BACKOFF_MILLIS * previousAttempt;
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(millis));
    }

    private String copyImage(OmrGradingCallbackRequest request, String permanentKey) {
        byte[] imageData = storagePort.getObject(request.imageKey());
        storagePort.putObject(permanentKey, imageData, "image/png");
        return permanentKey;
    }

    private void cleanupImages(Long jobId) {
        try {
            OmrGradingJob job = jobRepository.findById(jobId).orElse(null);
            if (job != null && job.getImageKeys() != null) {
                List<String> retainedSourceKeys = tx.getSourceImageKeysWithoutPermanentCopy(jobId);
                List<String> cleanupKeys = job.getImageKeys().stream()
                        .filter(key -> !retainedSourceKeys.contains(key))
                        .toList();
                if (!cleanupKeys.isEmpty()) {
                    storagePort.deleteObjects(cleanupKeys);
                    log.info("Cleaned up {} MinIO images for job: {}", cleanupKeys.size(), jobId);
                }
                if (!retainedSourceKeys.isEmpty()) {
                    log.warn("Retained {} OMR source images without a permanent copy for job: {}",
                            retainedSourceKeys.size(), jobId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup MinIO images for job: {}", jobId, e);
        }
    }

    /** All DB-touching operations, each in their own transaction scope. */
    @Component
    @RequiredArgsConstructor
    static class Tx {

        record SuccessPrep(String permanentKey) {}

        private final OmrGradingJobRepository jobRepository;
        private final OmrSubmissionService submissionService;

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
        public boolean finalizeIfDone(Long jobId, int totalImages) {
            return jobRepository.tryFinalize(jobId, totalImages);
        }

        @Transactional(readOnly = true)
        public SuccessPrep prepareSuccess(OmrGradingCallbackRequest request) {
            var submission = submissionService.getSubmission(request.jobId(), request.imageIndex());
            String tenantId = TenantContextHolder.toStorageTenantId(request.tenantSlug());
            String permanentKey = tenantId + "/omr/submissions/" + submission.getId() + ".png";
            return new SuccessPrep(permanentKey);
        }

        @Transactional
        public void saveSuccess(OmrGradingCallbackRequest request, SuccessPrep prep, String permanentKey) {
            submissionService.complete(request.jobId(), request.imageIndex(), request, permanentKey);
            jobRepository.incrementSuccess(request.jobId());
        }

        @Transactional
        public void recordFail(Long jobId, int imageIndex, String reason) {
            submissionService.fail(jobId, imageIndex, reason);
            jobRepository.incrementFail(jobId);
        }

        @Transactional(readOnly = true)
        public List<String> getSourceImageKeysWithoutPermanentCopy(Long jobId) {
            return submissionService.getSourceImageKeysWithoutPermanentCopy(jobId);
        }
    }
}
