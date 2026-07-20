package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.request.OmrGradingCallbackRequest;
import com.lumie.exam.application.port.out.CallbackDeduplicationPort;
import com.lumie.exam.application.port.out.OmrServicePort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.student.api.StudentService;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.OmrGradingJob;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.exam.domain.vo.JobStatus;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
                log.warn("Duplicate OMR callback skipped: jobId={}, imageIndex={}",
                        request.jobId(), request.imageIndex());
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

            if (counted && tx.finalizeIfDone(request.jobId(), request.totalImages())) {
                log.info("OMR grading job reached terminal state: jobId={}", request.jobId());
                cleanupImages(request.jobId());
            }
            return;
        }
    }

    private boolean recordCallbackBeforeFinalize(OmrGradingCallbackRequest request) {
        if (!tx.isOpenJob(request.jobId())) {
            log.warn("Late OMR callback skipped for terminal or missing job: jobId={}, imageIndex={}",
                    request.jobId(), request.imageIndex());
            return false;
        }

        tx.markProcessing(request.jobId());

        // A success callback without a detected phone cannot be attributed to a student
        // (it is the OMR identity key). Treat it as a failed sheet rather than persisting
        // a poisoned "010null" lookup key.
        boolean success = request.success() && hasPhoneNumber(request);
        if (request.success() && !success) {
            log.warn("OMR success callback missing phoneNumber, treating as failure: jobId={}, imageIndex={}",
                    request.jobId(), request.imageIndex());
        }

        if (success) {
            // Phase 1 (read tx): resolve in-memory ExamResult + compute storage key
            Tx.SuccessPrep prep = tx.prepareSuccess(request);
            prep.examResult().assignOmrGradingJob(request.jobId());

            // Phase 2 (no tx): copy image in MinIO
            String permanentKey = copyImage(request, prep.permanentKey());
            if (permanentKey != null) {
                prep.examResult().assignOmrImage(permanentKey);
            }

            // Phase 3 (write tx): save result + update counters
            try {
                tx.saveSuccess(request, prep);
            } catch (DataIntegrityViolationException e) {
                if (prep.studentOpt().isEmpty()) {
                    throw e;
                }
                Long studentId = prep.studentOpt().get().id();
                log.warn("OMR callback save raced with existing registered result, overwriting existing row: jobId={}, studentId={}",
                        request.jobId(), studentId, e);
                tx.overwriteRegisteredResult(
                        request,
                        studentId,
                        fullPhoneNumber(request),
                        prep.examResult().getOmrImageKey()
                );
            }
        } else {
            tx.recordFail(request.jobId());
        }

        return true;
    }

    private static boolean hasPhoneNumber(OmrGradingCallbackRequest request) {
        return request.phoneNumber() != null && !request.phoneNumber().isBlank();
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

    /** OMR sheets carry the phone without the leading "010"; restore it for matching/storage. */
    private static String fullPhoneNumber(OmrGradingCallbackRequest request) {
        return "010" + request.phoneNumber();
    }

    private String copyImage(OmrGradingCallbackRequest request, String permanentKey) {
        try {
            byte[] imageData = storagePort.getObject(request.imageKey());
            storagePort.putObject(permanentKey, imageData, "image/png");
            return permanentKey;
        } catch (Exception e) {
            log.warn("Failed to copy OMR image to permanent path: {}", permanentKey, e);
            return null;
        }
    }

    private void cleanupImages(Long jobId) {
        try {
            OmrGradingJob job = jobRepository.findById(jobId).orElse(null);
            if (job != null && job.getImageKeys() != null) {
                storagePort.deleteObjects(job.getImageKeys());
                log.info("Cleaned up {} MinIO images for job: {}", job.getImageKeys().size(), jobId);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup MinIO images for job: {}", jobId, e);
        }
    }

    /** All DB-touching operations, each in their own transaction scope. */
    @Component
    @RequiredArgsConstructor
    static class Tx {

        record SuccessPrep(
                Exam exam,
                Optional<StudentService.StudentData> studentOpt,
                ExamResult examResult,
                String permanentKey
        ) {}

        private final ExamRepository examRepository;
        private final ExamResultRepository resultRepository;
        private final ResultCommandService resultCommandService;
        private final OmrGradingJobRepository jobRepository;
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
        public boolean finalizeIfDone(Long jobId, int totalImages) {
            return jobRepository.tryFinalize(jobId, totalImages);
        }

        @Transactional(readOnly = true)
        public SuccessPrep prepareSuccess(OmrGradingCallbackRequest request) {
            Exam exam = examRepository.findById(request.examId())
                    .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));

            String fullPhone = fullPhoneNumber(request);
            String tenantSlug = TenantContextHolder.getRequiredTenant();
            Optional<StudentService.StudentData> studentOpt =
                    studentService.getStudentByPhone(tenantSlug, fullPhone);

            OmrServicePort.OmrGradingResult omrResult = toOmrGradingResult(request);
            ExamResult examResult = studentOpt.isPresent()
                    ? resultCommandService.resolveResult(exam, studentOpt.get().id(), omrResult)
                    : resultCommandService.resolveResultForUnregistered(exam, fullPhone, omrResult);

            String tenantId = TenantContextHolder.toStorageTenantId(request.tenantSlug());
            String imageId = studentOpt.map(s -> String.valueOf(s.id())).orElse(fullPhone);
            String permanentKey = tenantId + "/omr/" + request.examId() + "/" + imageId;

            return new SuccessPrep(exam, studentOpt, examResult, permanentKey);
        }

        @Transactional
        public void saveSuccess(OmrGradingCallbackRequest request, SuccessPrep prep) {
            ExamResult examResult = prep.examResult();

            // Registered results otherwise lack the phone; persist it here (inside the
            // write tx) so the results view can show it. Unregistered rows already carry
            // it as their identity. Done in the write tx — not readOnly prepareSuccess —
            // so the mutation is flushed reliably regardless of managed/detached state.
            if (prep.studentOpt().isPresent()) {
                examResult.assignPhoneNumber(fullPhoneNumber(request));
            }

            resultRepository.saveAndFlush(examResult);
            jobRepository.incrementSuccess(request.jobId());
        }

        @Transactional
        public void recordFail(Long jobId) {
            jobRepository.incrementFail(jobId);
        }

        @Transactional
        public void overwriteRegisteredResult(
                OmrGradingCallbackRequest request,
                Long studentId,
                String phoneNumber,
                String omrImageKey
        ) {
            Exam exam = examRepository.findById(request.examId())
                    .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));
            ExamResult examResult = resultCommandService.resolveResult(exam, studentId, toOmrGradingResult(request));
            examResult.assignPhoneNumber(phoneNumber);
            examResult.assignOmrGradingJob(request.jobId());
            examResult.assignOmrImage(omrImageKey);
            resultRepository.saveAndFlush(examResult);
            jobRepository.incrementSuccess(request.jobId());
        }

        private OmrServicePort.OmrGradingResult toOmrGradingResult(OmrGradingCallbackRequest request) {
            List<OmrServicePort.OmrQuestionResult> questions = request.results().stream()
                    .map(q -> new OmrServicePort.OmrQuestionResult(
                            q.questionNumber(), q.studentAnswer(), q.correctAnswer(),
                            q.score(), q.earnedScore(), q.questionType()))
                    .toList();
            return new OmrServicePort.OmrGradingResult(
                    request.totalScore(), request.grade(), request.phoneNumber(), questions);
        }
    }
}
