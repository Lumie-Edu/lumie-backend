package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.message.OmrGradingJobMessage;
import com.lumie.exam.application.dto.request.OmrBatchConfirmRequest;
import com.lumie.exam.application.dto.request.OmrPresignRequest;
import com.lumie.exam.application.dto.response.ExamResultResponse;
import com.lumie.exam.application.dto.response.OmrJobResponse;
import com.lumie.exam.application.dto.response.OmrPresignResponse;
import com.lumie.exam.application.port.out.BillingServicePort;
import com.lumie.exam.application.port.out.OmrServicePort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.application.port.out.StudentServicePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.OmrGradingJob;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.exam.domain.service.GradeCalculator;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.messaging.config.RabbitMqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResultCommandService {

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final OmrGradingJobRepository jobRepository;
    private final GradeCalculator gradeCalculator;
    private final StudentServicePort studentService;
    private final BillingServicePort billingService;
    private final OmrServicePort omrService;
    private final OmrStoragePort storagePort;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public ExamResultResponse processOmrGrading(Long examId, Long studentId, byte[] imageData) {
        log.info("Processing OMR grading for exam: {}, student: {}", examId, studentId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        if (!studentService.existsById(studentId)) {
            throw new ExamException(ExamErrorCode.STUDENT_NOT_FOUND);
        }

        if (!billingService.hasOmrQuota()) {
            throw new ExamException(ExamErrorCode.OMR_QUOTA_EXCEEDED);
        }

        OmrServicePort.OmrGradingResult omrResult;
        try {
            omrResult = omrService.gradeOmrImage(
                    imageData,
                    exam.getCorrectAnswers(),
                    exam.getQuestionScores(),
                    exam.getQuestionTypes()
            );
        } catch (Exception e) {
            log.error("OMR grading failed", e);
            throw new ExamException(ExamErrorCode.OMR_GRADING_FAILED);
        }

        billingService.consumeOmrQuota();

        ExamResult result = ExamResult.create(exam, studentId, omrResult.totalScore());

        for (OmrServicePort.OmrQuestionResult qr : omrResult.results()) {
            QuestionResult questionResult = QuestionResult.create(
                    qr.questionNumber(),
                    qr.studentAnswer(),
                    qr.correctAnswer(),
                    qr.score()
            );
            result.addQuestionResult(questionResult);
        }

        // 절대평가일 때만 등급 계산하여 저장 (상대평가는 조회 시 계산)
        gradeCalculator.calculateAndAssignGradeIfAbsolute(exam, result);

        ExamResult savedResult = resultRepository.save(result);

        log.info("OMR grading completed for exam: {}, student: {}", examId, studentId);
        return ExamResultResponse.from(savedResult);
    }

    public OmrPresignResponse generatePresignedUrls(Long examId, OmrPresignRequest request) {
        log.info("Generating presigned URLs for exam: {}, files: {}", examId, request.fileNames().size());

        examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        String tenantId = extractTenantId(tenantSlug);
        String batchKey = java.util.UUID.randomUUID().toString().substring(0, 8);

        List<OmrPresignResponse.PresignedUpload> uploads = request.fileNames().stream()
                .map(fileName -> {
                    String sanitized = sanitizeFileName(fileName);
                    String objectKey = "omr/tmp/" + tenantId + "/" + batchKey + "/" + sanitized;
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
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        String tenantId = extractTenantId(tenantSlug);

        // Validate object keys
        for (String key : request.objectKeys()) {
            if (!key.startsWith("omr/tmp/" + tenantId + "/") || key.contains("..")) {
                throw new ExamException(ExamErrorCode.INVALID_EXAM_DATA);
            }
        }

        List<String> imageKeys = request.objectKeys();
        OmrGradingJob job = OmrGradingJob.create(exam, tenantSlug, imageKeys.size(), imageKeys);
        OmrGradingJob savedJob = jobRepository.save(job);

        OmrGradingJobMessage message = new OmrGradingJobMessage(
                savedJob.getId(), examId, tenantSlug, imageKeys);
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        rabbitTemplate.convertAndSend(
                                RabbitMqConstants.LUMIE_COMMANDS_EXCHANGE,
                                RabbitMqConstants.GRADING_OMR_REQUEST_ROUTING_KEY,
                                message);
                        log.info("Async OMR grading job submitted: jobId={}", savedJob.getId());
                    }
                });

        return new OmrJobResponse(savedJob.getId());
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        return fileName.replaceAll("[^a-zA-Z0-9가-힣._-]", "_");
    }

    private String extractTenantId(String tenantSlug) {
        if (tenantSlug.startsWith("inst-")) {
            return tenantSlug.substring(5);
        }
        return tenantSlug;
    }
}
