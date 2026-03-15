package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.message.OmrGradingJobMessage;
import com.lumie.exam.application.dto.response.BatchOmrGradingResponse;
import com.lumie.exam.application.dto.response.ExamResultResponse;
import com.lumie.exam.application.dto.response.OmrJobResponse;
import com.lumie.exam.application.port.out.BillingServicePort;
import com.lumie.exam.application.port.out.ExamEventPublisherPort;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private final ExamEventPublisherPort eventPublisher;
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

        String tenantSlug = TenantContextHolder.getTenantSlug();
        eventPublisher.publishResultSubmitted(savedResult, tenantSlug);

        log.info("OMR grading completed for exam: {}, student: {}", examId, studentId);
        return ExamResultResponse.from(savedResult);
    }

    @Transactional
    public BatchOmrGradingResponse processBatchOmrGrading(Long examId, List<MultipartFile> images) {
        log.info("Processing batch OMR grading for exam: {}, images: {}", examId, images.size());

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        String tenantSlug = TenantContextHolder.getTenantSlug();
        List<BatchOmrGradingResponse.BatchOmrResult> results = new ArrayList<>();
        int savedCount = 0;

        for (MultipartFile image : images) {
            String fileName = image.getOriginalFilename();
            try {
                byte[] imageData = image.getBytes();

                OmrServicePort.OmrGradingResult omrResult = omrService.gradeOmrImage(
                        imageData,
                        exam.getCorrectAnswers(),
                        exam.getQuestionScores(),
                        exam.getQuestionTypes()
                );

                String fullPhone = "010" + omrResult.phoneNumber();
                Optional<StudentServicePort.StudentInfo> studentOpt = studentService.findByPhone(fullPhone);

                if (studentOpt.isEmpty()) {
                    results.add(BatchOmrGradingResponse.BatchOmrResult.gradedButNotSaved(
                            fileName, omrResult.phoneNumber(), omrResult.totalScore(), omrResult.grade(),
                            "학생을 찾을 수 없음: " + fullPhone));
                    continue;
                }

                StudentServicePort.StudentInfo student = studentOpt.get();

                ExamResult result = ExamResult.create(exam, student.id(), omrResult.totalScore());
                for (OmrServicePort.OmrQuestionResult qr : omrResult.results()) {
                    QuestionResult questionResult = QuestionResult.create(
                            qr.questionNumber(),
                            qr.studentAnswer(),
                            qr.correctAnswer(),
                            qr.score()
                    );
                    result.addQuestionResult(questionResult);
                }

                gradeCalculator.calculateAndAssignGradeIfAbsolute(exam, result);
                ExamResult savedResult = resultRepository.save(result);
                eventPublisher.publishResultSubmitted(savedResult, tenantSlug);
                billingService.consumeOmrQuota();

                results.add(BatchOmrGradingResponse.BatchOmrResult.success(
                        fileName, omrResult.phoneNumber(), student.id(), student.name(),
                        omrResult.totalScore(), omrResult.grade(), true, savedResult.getId()));
                savedCount++;

            } catch (Exception e) {
                log.error("Failed to process OMR image: {}", fileName, e);
                results.add(BatchOmrGradingResponse.BatchOmrResult.failure(fileName, e.getMessage()));
            }
        }

        int successCount = (int) results.stream().filter(BatchOmrGradingResponse.BatchOmrResult::success).count();
        int failCount = results.size() - successCount;

        log.info("Batch OMR grading completed for exam: {}, total: {}, success: {}, saved: {}",
                examId, images.size(), successCount, savedCount);

        return new BatchOmrGradingResponse(images.size(), successCount, failCount, savedCount, results);
    }

    @Transactional
    public OmrJobResponse submitBatchOmrGrading(Long examId, List<MultipartFile> images) {
        log.info("Submitting async batch OMR grading for exam: {}, images: {}", examId, images.size());

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        String tenantId = extractTenantId(tenantSlug);
        String batchKey = java.util.UUID.randomUUID().toString().substring(0, 8);

        // Upload images to MinIO in parallel
        List<String> imageKeys = new CopyOnWriteArrayList<>();
        List<CompletableFuture<Void>> futures = images.stream()
                .map(image -> CompletableFuture.runAsync(() -> {
                    try {
                        String objectKey = "omr/tmp/" + tenantId + "/" + batchKey + "/" + image.getOriginalFilename();
                        storagePort.putObject(objectKey, image.getBytes(), image.getContentType());
                        imageKeys.add(objectKey);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to upload: " + image.getOriginalFilename(), e);
                    }
                }))
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.error("Failed to upload images to MinIO", e);
            storagePort.deleteObjects(imageKeys);
            throw new ExamException(ExamErrorCode.OMR_GRADING_FAILED);
        }

        // Create job with image keys
        OmrGradingJob job = OmrGradingJob.create(exam, tenantSlug, images.size(), imageKeys);
        OmrGradingJob savedJob = jobRepository.save(job);

        // Publish message to RabbitMQ after transaction commits
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

    private String extractTenantId(String tenantSlug) {
        if (tenantSlug.startsWith("inst-")) {
            return tenantSlug.substring(5);
        }
        return tenantSlug;
    }
}
