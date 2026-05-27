package com.lumie.exam.application.service;

import com.lumie.exam.application.event.OmrGradingRequestedEvent;
import com.lumie.exam.application.dto.request.OmrBatchConfirmRequest;
import com.lumie.exam.application.dto.request.OmrPresignRequest;
import com.lumie.exam.application.dto.request.UpdateQuestionAnswerRequest;
import com.lumie.exam.application.dto.response.QuestionResultResponse;
import com.lumie.exam.application.dto.response.ExamResultResponse;
import com.lumie.exam.application.dto.response.OmrJobResponse;
import com.lumie.exam.application.dto.response.OmrPresignResponse;
import com.lumie.exam.application.port.out.OmrServicePort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.billing.api.BillingService;
import com.lumie.student.api.StudentService;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.OmrGradingJob;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.quota.MetricType;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResultCommandService {

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final OmrGradingJobRepository jobRepository;
    private final GradeCalculator gradeCalculator;
    private final StudentService studentService;
    private final BillingService billingService;
    private final OmrServicePort omrService;
    private final OmrStoragePort storagePort;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public ExamResultResponse processOmrGrading(Long examId, Long studentId, byte[] imageData) {
        log.info("Processing OMR grading for exam: {}, student: {}", examId, studentId);

        // Phase 1 — validate (each cross-module call self-manages its own short tx).
        // Held outside the persistence transaction so the long-running OMR HTTP
        // call below does not pin a Hikari connection.
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        Exam exam = transactionTemplate.execute(status ->
                examRepository.findById(examId)
                        .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND)));
        if (!studentService.validateStudent(tenantSlug, studentId).valid()) {
            throw new BusinessException(ExamErrorCode.STUDENT_NOT_FOUND);
        }
        if (!billingService.checkQuota(tenantSlug, MetricType.OMR_MONTHLY).allowed()) {
            throw new BusinessException(ExamErrorCode.OMR_QUOTA_EXCEEDED);
        }

        // Phase 2 — external HTTP grading. No tx held during this call (HARD_RULES).
        OmrServicePort.OmrGradingResult omrResult;
        try {
            omrResult = omrService.gradeOmrImage(
                    imageData,
                    tenantSlug,
                    exam.getCorrectAnswers(),
                    exam.getQuestionScores(),
                    exam.getQuestionTypes()
            );
        } catch (Exception e) {
            log.error("OMR grading failed", e);
            throw new BusinessException(ExamErrorCode.OMR_GRADING_FAILED);
        }

        // Phase 3 — persist results in a fresh write transaction.
        // Re-fetch exam inside the tx so resolveResult works on a managed entity.
        ExamResultResponse response = transactionTemplate.execute(status -> {
            Exam managedExam = examRepository.findById(examId)
                    .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));
            ExamResult result = resolveResult(examId, managedExam, studentId, omrResult);
            ExamResult saved = resultRepository.save(result);
            return ExamResultResponse.from(saved);
        });

        log.info("OMR grading completed for exam: {}, student: {}", examId, studentId);
        return response;
    }

    @Transactional(readOnly = true)
    public OmrPresignResponse generatePresignedUrls(Long examId, OmrPresignRequest request) {
        log.info("Generating presigned URLs for exam: {}, files: {}", examId, request.fileNames().size());

        examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        String tenantId = TenantContextHolder.toStorageTenantId(tenantSlug);
        String batchKey = java.util.UUID.randomUUID().toString().substring(0, 8);

        List<OmrPresignResponse.PresignedUpload> uploads = request.fileNames().stream()
                .map(fileName -> {
                    String sanitized = sanitizeFileName(fileName);
                    String objectKey = "tmp/" + tenantId + "/omr/" + batchKey + "/" + sanitized;
                    String uploadUrl = storagePort.getPresignedPutUrl(objectKey, 900);
                    return new OmrPresignResponse.PresignedUpload(fileName, objectKey, uploadUrl);
                })
                .toList();

        log.info("Generated {} presigned URLs for exam: {}, batchKey: {}", uploads.size(), examId, batchKey);
        return new OmrPresignResponse(batchKey, uploads);
    }

    @Transactional
    public OmrJobResponse confirmBatchOmrGrading(Long examId, OmrBatchConfirmRequest request) {
        log.info("Confirming batch OMR grading for exam: {}, keys: {}", examId, request.objectKeys().size());

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        String tenantId = TenantContextHolder.toStorageTenantId(tenantSlug);

        // Validate object keys
        for (String key : request.objectKeys()) {
            if (!key.startsWith("tmp/" + tenantId + "/omr/") || key.contains("..")) {
                throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA);
            }
        }

        List<String> imageKeys = request.objectKeys();
        OmrGradingJob job = OmrGradingJob.create(exam, tenantSlug, imageKeys.size(), imageKeys);
        OmrGradingJob savedJob = jobRepository.save(job);

        // Outbox: each event is persisted to event_publication in THIS transaction,
        // atomically with the job row, then forwarded to RabbitMQ after commit by
        // GradingRequestForwarder. Closes the commit→broker gap.
        for (int i = 0; i < imageKeys.size(); i++) {
            eventPublisher.publishEvent(new OmrGradingRequestedEvent(
                    savedJob.getId(), examId, tenantSlug,
                    imageKeys.get(i), i, imageKeys.size()));
        }
        log.info("Async OMR grading job queued: jobId={}, images={}",
                savedJob.getId(), imageKeys.size());

        return new OmrJobResponse(savedJob.getId());
    }

    @Transactional
    public QuestionResultResponse updateQuestionAnswer(
            Long questionResultId, UpdateQuestionAnswerRequest request) {
        log.info("Updating question answer: questionResultId={}, newChoice={}",
                questionResultId, request.selectedChoice());

        ExamResult result = resultRepository.findByQuestionResultId(questionResultId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));

        QuestionResult updated = result.updateQuestionAnswer(questionResultId, request.selectedChoice());
        gradeCalculator.calculateAndAssignGradeIfAbsolute(result.getExam(), result);

        ExamResult saved = resultRepository.save(result);
        QuestionResult refreshed = saved.getQuestionResults().stream()
                .filter(qr -> qr.getId().equals(updated.getId()))
                .findFirst()
                .orElse(updated);
        return QuestionResultResponse.from(refreshed, saved.getExam());
    }

    @Transactional
    public void deleteResult(Long examId, Long resultId) {
        log.info("Deleting exam result: examId={}, resultId={}", examId, resultId);

        ExamResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));

        if (!result.getExam().getId().equals(examId)) {
            throw new BusinessException(ExamErrorCode.RESULT_NOT_FOUND);
        }

        if (result.getOmrImageKey() != null) {
            try {
                storagePort.deleteObjects(java.util.List.of(result.getOmrImageKey()));
            } catch (Exception e) {
                log.warn("Failed to delete OMR image from MinIO: {}", result.getOmrImageKey(), e);
            }
        }

        resultRepository.deleteById(resultId);
    }

    public ExamResult resolveResult(Exam exam, Long studentId, OmrServicePort.OmrGradingResult omrResult) {
        return resolveResult(exam.getId(), exam, studentId, omrResult);
    }

    public ExamResult resolveResult(Long examId, Exam exam, Long studentId, OmrServicePort.OmrGradingResult omrResult) {
        List<ExamResult.OmrQuestionUpdate> updates = omrResult.results().stream()
                .map(qr -> new ExamResult.OmrQuestionUpdate(
                        qr.questionNumber(), qr.studentAnswer(), qr.correctAnswer(), qr.score()))
                .toList();

        ExamResult result = resultRepository.findByExamIdAndStudentId(examId, studentId)
                .map(existing -> {
                    existing.regrade(omrResult.totalScore(), updates);
                    return existing;
                })
                .orElseGet(() -> {
                    ExamResult newResult = ExamResult.create(exam, studentId, omrResult.totalScore());
                    for (OmrServicePort.OmrQuestionResult qr : omrResult.results()) {
                        newResult.addQuestionResult(QuestionResult.create(
                                qr.questionNumber(), qr.studentAnswer(), qr.correctAnswer(), qr.score()));
                    }
                    return newResult;
                });

        gradeCalculator.calculateAndAssignGradeIfAbsolute(exam, result);
        return result;
    }

    public ExamResult resolveResultForUnregistered(Exam exam, String phoneNumber, OmrServicePort.OmrGradingResult omrResult) {
        List<ExamResult.OmrQuestionUpdate> updates = omrResult.results().stream()
                .map(qr -> new ExamResult.OmrQuestionUpdate(
                        qr.questionNumber(), qr.studentAnswer(), qr.correctAnswer(), qr.score()))
                .toList();

        ExamResult result = resultRepository.findByExamIdAndPhoneNumber(exam.getId(), phoneNumber)
                .map(existing -> {
                    existing.regrade(omrResult.totalScore(), updates);
                    return existing;
                })
                .orElseGet(() -> {
                    ExamResult newResult = ExamResult.createForUnregistered(exam, phoneNumber, omrResult.totalScore());
                    for (OmrServicePort.OmrQuestionResult qr : omrResult.results()) {
                        newResult.addQuestionResult(QuestionResult.create(
                                qr.questionNumber(), qr.studentAnswer(), qr.correctAnswer(), qr.score()));
                    }
                    return newResult;
                });

        gradeCalculator.calculateAndAssignGradeIfAbsolute(exam, result);
        return result;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        return fileName.replaceAll("[^a-zA-Z0-9가-힣._-]", "_");
    }

}
