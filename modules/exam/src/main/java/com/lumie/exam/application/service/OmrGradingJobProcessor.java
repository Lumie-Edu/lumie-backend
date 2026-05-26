package com.lumie.exam.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OmrGradingJobProcessor {

    private static final String DEDUP_KEY_PREFIX = "omr:cb:";

    private final OmrStoragePort storagePort;
    private final CallbackDeduplicationPort callbackDeduplication;
    private final OmrGradingJobRepository jobRepository;
    private final ObjectMapper objectMapper;
    private final Tx tx;

    public void processGradingCallback(OmrGradingCallbackRequest request) {
        log.info("Processing grading callback: jobId={}, imageIndex={}/{}, success={}",
                request.jobId(), request.imageIndex() + 1, request.totalImages(), request.success());

        String dedupKey = DEDUP_KEY_PREFIX + request.jobId() + ":" + request.imageIndex();
        if (!callbackDeduplication.markIfAbsent(dedupKey)) {
            log.warn("Duplicate OMR callback skipped: jobId={}, imageIndex={}",
                    request.jobId(), request.imageIndex());
            return;
        }

        tx.markProcessing(request.jobId());

        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("fileName", extractFileName(request.imageKey()));

        if (request.success()) {
            // Phase 1 (read tx): resolve in-memory ExamResult + compute storage key
            Tx.SuccessPrep prep = tx.prepareSuccess(request);

            // Phase 2 (no tx): copy image in MinIO
            String permanentKey = copyImage(request, prep.permanentKey());
            if (permanentKey != null) {
                prep.examResult().assignOmrImage(permanentKey);
            }

            // Phase 3 (write tx): save result + update counters
            Long savedResultId = tx.saveSuccess(request, prep, resultMap);

            resultMap.put("success", true);
            resultMap.put("saved", true);
            resultMap.put("phoneNumber", request.phoneNumber());
            resultMap.put("totalScore", request.totalScore());
            resultMap.put("grade", request.grade());
            resultMap.put("resultId", savedResultId);
        } else {
            resultMap.put("success", false);
            resultMap.put("saved", false);
            resultMap.put("error", request.error());
            tx.recordFail(request.jobId());
        }

        appendAndMaybeComplete(request, resultMap);
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

    private void appendAndMaybeComplete(OmrGradingCallbackRequest request, Map<String, Object> resultMap) {
        try {
            String json = objectMapper.writeValueAsString(List.of(resultMap));
            jobRepository.appendResult(request.jobId(), json);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize result JSON for jobId={}: {}", request.jobId(), e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to append result for jobId={}: {}", request.jobId(), e.getMessage());
        }

        if (jobRepository.tryComplete(request.jobId(), request.totalImages())) {
            log.info("OMR grading job completed: jobId={}", request.jobId());
            cleanupImages(request.jobId());
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

    private String extractFileName(String objectKey) {
        int lastSlash = objectKey.lastIndexOf('/');
        return lastSlash >= 0 ? objectKey.substring(lastSlash + 1) : objectKey;
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
        public SuccessPrep prepareSuccess(OmrGradingCallbackRequest request) {
            Exam exam = examRepository.findById(request.examId())
                    .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));

            String fullPhone = "010" + request.phoneNumber();
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
        public Long saveSuccess(OmrGradingCallbackRequest request, SuccessPrep prep,
                                Map<String, Object> resultMap) {
            OmrServicePort.OmrGradingResult omrResult = toOmrGradingResult(request);
            ExamResult examResult = prep.examResult();

            ExamResult savedResult;
            try {
                savedResult = resultRepository.save(examResult);
            } catch (BusinessException e) {
                if (e.getErrorCode() != ExamErrorCode.DUPLICATE_RESULT) {
                    throw e;
                }
                log.warn("Duplicate result detected, retrying with existing record: jobId={}", request.jobId());
                String fullPhone = "010" + request.phoneNumber();
                examResult = prep.studentOpt().isPresent()
                        ? resultCommandService.resolveResult(prep.exam(), prep.studentOpt().get().id(), omrResult)
                        : resultCommandService.resolveResultForUnregistered(prep.exam(), fullPhone, omrResult);
                savedResult = resultRepository.save(examResult);
            }

            if (prep.studentOpt().isPresent()) {
                resultMap.put("studentId", prep.studentOpt().get().id());
                resultMap.put("studentName", prep.studentOpt().get().studentName());
            }

            jobRepository.incrementSuccess(request.jobId());
            return savedResult.getId();
        }

        @Transactional
        public void recordFail(Long jobId) {
            jobRepository.incrementFail(jobId);
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
