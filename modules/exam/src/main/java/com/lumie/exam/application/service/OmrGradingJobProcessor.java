package com.lumie.exam.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.exam.application.dto.request.OmrGradingCallbackRequest;
import com.lumie.exam.application.port.out.OmrServicePort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.student.api.StudentService;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.OmrGradingJob;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import com.lumie.exam.domain.exception.DuplicateExamResultException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OmrGradingJobProcessor {

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final ResultCommandService resultCommandService;
    private final OmrStoragePort storagePort;
    private final StudentService studentService;
    private final OmrGradingJobRepository jobRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processGradingCallback(OmrGradingCallbackRequest request) {
        log.info("Processing grading callback: jobId={}, imageIndex={}/{}, success={}",
                request.jobId(), request.imageIndex() + 1, request.totalImages(), request.success());

        // Tenant context is set by RequestContextFilter from X-Tenant-Slug header
        jobRepository.startProcessingIfPending(request.jobId());

        Map<String, Object> resultMap = new LinkedHashMap<>();
        String fileName = extractFileName(request.imageKey());
        resultMap.put("fileName", fileName);

        if (request.success()) {
            processSuccessResult(request, resultMap);
            jobRepository.incrementSuccess(request.jobId());
        } else {
            resultMap.put("success", false);
            resultMap.put("saved", false);
            resultMap.put("error", request.error());
            jobRepository.incrementFail(request.jobId());
        }

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

    private void processSuccessResult(OmrGradingCallbackRequest request, Map<String, Object> resultMap) {
        Exam exam = examRepository.findById(request.examId())
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        OmrServicePort.OmrGradingResult omrResult = toOmrGradingResult(request);

        String fullPhone = "010" + request.phoneNumber();
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Optional<StudentService.StudentData> studentOpt =
                studentService.getStudentByPhone(tenantSlug, fullPhone);

        ExamResult examResult;
        if (studentOpt.isPresent()) {
            StudentService.StudentData student = studentOpt.get();
            examResult = resultCommandService.resolveResult(exam, student.id(), omrResult);
            resultMap.put("studentId", student.id());
            resultMap.put("studentName", student.studentName());
        } else {
            examResult = resultCommandService.resolveResultForUnregistered(exam, fullPhone, omrResult);
        }

        String tenantId = TenantContextHolder.toStorageTenantId(request.tenantSlug());
        String imageId = studentOpt.map(s -> String.valueOf(s.id())).orElse(fullPhone);
        String permanentKey = tenantId + "/omr/" + request.examId() + "/" + imageId;
        try {
            byte[] imageData = storagePort.getObject(request.imageKey());
            storagePort.putObject(permanentKey, imageData, "image/png");
            examResult.setOmrImageKey(permanentKey);
        } catch (Exception e) {
            log.warn("Failed to copy OMR image to permanent path: {}", permanentKey, e);
        }

        ExamResult savedResult;
        try {
            savedResult = resultRepository.save(examResult);
        } catch (DuplicateExamResultException e) {
            log.warn("Duplicate result detected, retrying with existing record: jobId={}", request.jobId());
            if (studentOpt.isPresent()) {
                examResult = resultCommandService.resolveResult(exam, studentOpt.get().id(), omrResult);
            } else {
                examResult = resultCommandService.resolveResultForUnregistered(exam, fullPhone, omrResult);
            }
            savedResult = resultRepository.save(examResult);
        }
        // Quota consumption is recorded by billing-svc usage tracking.

        resultMap.put("success", true);
        resultMap.put("saved", true);
        resultMap.put("phoneNumber", request.phoneNumber());
        resultMap.put("totalScore", request.totalScore());
        resultMap.put("grade", request.grade());
        resultMap.put("resultId", savedResult.getId());
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
}
