package com.lumie.exam.application.service;

import com.lumie.exam.application.event.OmrGradingRequestedEvent;
import com.lumie.exam.api.ExamService;
import com.lumie.exam.application.dto.request.OmrBatchConfirmRequest;
import com.lumie.exam.application.dto.request.OmrPresignRequest;
import com.lumie.exam.application.dto.request.UpdateResultPhoneRequest;
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
import com.lumie.common.util.PhoneNumberUtils;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import com.lumie.exam.domain.repository.OmrSubmissionRepository;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResultCommandService {

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final OmrGradingJobRepository jobRepository;
    private final OmrSubmissionRepository submissionRepository;
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
        // validateStudent also rejects inactive students; keep it as the gate.
        if (!studentService.validateStudent(tenantSlug, studentId).valid()) {
            throw new BusinessException(ExamErrorCode.STUDENT_NOT_FOUND);
        }
        // The student is selected manually by id here, so the authoritative phone is the
        // registered student's own number (not OCR output). Fetched outside the tx.
        StudentService.StudentData student = studentService.getStudent(tenantSlug, studentId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.STUDENT_NOT_FOUND));
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
            result.assignPhoneNumber(student.studentPhone());
            ExamResult saved = resultRepository.save(result);
            return ExamResultResponse.from(saved);
        });

        log.info("OMR grading completed for exam: {}, student: {}", examId, studentId);
        return response;
    }

    @Transactional(readOnly = true)
    public OmrPresignResponse generatePresignedUrls(Long examId, OmrPresignRequest request) {
        log.info("Generating presigned URLs for exam: {}, files: {}", examId, request.fileNames().size());

        if (examRepository.findById(examId).isEmpty()) {
            throw new BusinessException(ExamErrorCode.EXAM_NOT_FOUND);
        }

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
        Instant batchAcceptedAt = Instant.now();
        OmrGradingJob job = OmrGradingJob.create(exam, tenantSlug, imageKeys.size(), imageKeys);
        OmrGradingJob savedJob = jobRepository.save(job);

        List<com.lumie.exam.domain.entity.OmrSubmission> submissions = java.util.stream.IntStream
                .range(0, imageKeys.size())
                .mapToObj(index -> com.lumie.exam.domain.entity.OmrSubmission.pending(
                        exam, savedJob.getId(), index, imageKeys.get(index), batchAcceptedAt))
                .toList();
        submissionRepository.saveAll(submissions);

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
        result.recordManualOverride(java.time.Instant.now());

        ExamResult saved = resultRepository.save(result);
        QuestionResult refreshed = saved.getQuestionResults().stream()
                .filter(qr -> qr.getId().equals(updated.getId()))
                .findFirst()
                .orElse(updated);
        return QuestionResultResponse.from(refreshed, saved.getExam());
    }

    @Transactional
    public ExamService.ManualGradeResult gradeManualAnswers(Long examId, Long studentId, Map<String, String> answers) {
        if (studentId == null) {
            throw new BusinessException(ExamErrorCode.STUDENT_NOT_FOUND);
        }
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));
        String tenantSlug = TenantContextHolder.getRequiredTenant();
        if (!studentService.validateStudent(tenantSlug, studentId).valid()) {
            throw new BusinessException(ExamErrorCode.STUDENT_NOT_FOUND);
        }
        StudentService.StudentData student = studentService.getStudent(tenantSlug, studentId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.STUDENT_NOT_FOUND));
        OmrServicePort.OmrGradingResult gradingResult = buildManualGradingResult(exam, answers);
        ExamResult result = resolveResult(examId, exam, studentId, gradingResult);
        result.assignPhoneNumber(student.studentPhone());
        result.recordManualOverride(java.time.Instant.now());
        ExamResult saved = resultRepository.save(result);
        return toManualGradeResult(saved, exam, studentId);
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

    @Transactional
    public ExamResultResponse updateResultPhone(Long examId, Long resultId, UpdateResultPhoneRequest request) {
        String normalizedPhone = PhoneNumberUtils.toDigits(request.phoneNumber());
        if (!PhoneNumberUtils.isValidStudentPhone(normalizedPhone)) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA,
                    "전화번호 형식이 올바르지 않습니다. (예: 01012345678)");
        }

        ExamResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));
        if (!result.getExam().getId().equals(examId)
                || result.isStudentDeleted()
                || result.getStudentId() != null) {
            throw new BusinessException(ExamErrorCode.RESULT_NOT_FOUND);
        }

        String tenantSlug = TenantContextHolder.getRequiredTenant();
        StudentService.StudentData matchedStudent = studentService.getStudentByPhone(tenantSlug, normalizedPhone)
                .map(student -> {
                    if (!student.isActive()) {
                        throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA,
                                "이 전화번호는 비활성 학생 기록에 존재합니다.");
                    }
                    return student;
                })
                .orElse(null);

        if (matchedStudent != null) {
            resultRepository.findByExamIdAndStudentId(examId, matchedStudent.id())
                    .filter(existing -> !existing.getId().equals(resultId))
                    .ifPresent(existing -> {
                        throw new BusinessException(ExamErrorCode.DUPLICATE_RESULT,
                                "이미 해당 학생의 시험 성적이 있습니다.");
                    });
        }

        try {
            result.updatePhoneMatch(normalizedPhone, matchedStudent != null ? matchedStudent.id() : null);
            result.recordManualOverride(Instant.now());
            ExamResult saved = resultRepository.saveAndFlush(result);
            return ExamResultResponse.from(saved, matchedStudent != null ? matchedStudent.studentName() : null);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ExamErrorCode.DUPLICATE_RESULT,
                    "이미 해당 학생의 시험 성적이 있습니다.", e);
        }
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
        ExamResult result = ExamResult.createForUnregistered(exam, phoneNumber, omrResult.totalScore());
        for (OmrServicePort.OmrQuestionResult qr : omrResult.results()) {
            result.addQuestionResult(QuestionResult.create(
                    qr.questionNumber(), qr.studentAnswer(), qr.correctAnswer(), qr.score()));
        }

        gradeCalculator.calculateAndAssignGradeIfAbsolute(exam, result);
        return result;
    }

    private OmrServicePort.OmrGradingResult buildManualGradingResult(Exam exam, Map<String, String> answers) {
        if (answers == null) {
            throw new BusinessException(ExamErrorCode.INVALID_ANSWER_DATA, "Answers are required");
        }
        validateManualAnswerKeys(exam, answers);

        List<OmrServicePort.OmrQuestionResult> questions = java.util.stream.IntStream
                .rangeClosed(1, exam.getTotalQuestions())
                .mapToObj(questionNumber -> {
                    String key = String.valueOf(questionNumber);
                    String selected = normalizeManualAnswer(answers.get(key));
                    String correct = exam.getCorrectAnswerForQuestion(questionNumber);
                    int maxScore = exam.getScoreForQuestion(questionNumber);
                    int earnedScore = Objects.equals(selected, correct) ? maxScore : 0;
                    return new OmrServicePort.OmrQuestionResult(
                            questionNumber,
                            selected,
                            correct,
                            maxScore,
                            earnedScore,
                            exam.getQuestionType(questionNumber)
                    );
                })
                .toList();

        int totalScore = questions.stream()
                .mapToInt(OmrServicePort.OmrQuestionResult::earnedScore)
                .sum();
        return new OmrServicePort.OmrGradingResult(totalScore, 0, null, questions);
    }

    private void validateManualAnswerKeys(Exam exam, Map<String, String> answers) {
        for (String key : answers.keySet()) {
            int questionNumber;
            try {
                questionNumber = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                throw new BusinessException(ExamErrorCode.INVALID_ANSWER_DATA,
                        "Question number must be numeric: " + key);
            }

            if (questionNumber < 1 || questionNumber > exam.getTotalQuestions()) {
                throw new BusinessException(ExamErrorCode.INVALID_ANSWER_DATA,
                        "Question number out of range: " + questionNumber);
            }
        }
    }

    private String normalizeManualAnswer(String selected) {
        if (selected == null) {
            return null;
        }
        String normalized = selected.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private ExamService.ManualGradeResult toManualGradeResult(ExamResult result, Exam exam, Long studentId) {
        List<ExamService.QuestionGradeData> questionResults = result.getQuestionResults().stream()
                .sorted(java.util.Comparator.comparing(QuestionResult::getQuestionNumber))
                .map(questionResult -> {
                    Integer questionNumber = questionResult.getQuestionNumber();
                    return new ExamService.QuestionGradeData(
                            questionResult.getId(),
                            questionNumber,
                            questionResult.getSelectedChoice(),
                            questionResult.isCorrect(),
                            questionResult.getScore(),
                            questionResult.getEarnedScore(),
                            exam.getCorrectAnswerForQuestion(questionNumber),
                            exam.getQuestionType(questionNumber)
                    );
                })
                .toList();

        Integer grade = result.getGrade();
        if (exam.getCategory() == ExamCategory.GRADED && exam.getGradingType() == GradingType.ABSOLUTE) {
            GradeScale gradeScale = exam.getGradeScale() != null ? exam.getGradeScale() : GradeScale.NINE_GRADE;
            grade = gradeCalculator.calculateGradeForScore(
                    result.getTotalScore(), List.of(), GradingType.ABSOLUTE, gradeScale);
        } else if (exam.getCategory() == ExamCategory.GRADED && exam.getGradingType() == GradingType.RELATIVE) {
            GradeScale gradeScale = exam.getGradeScale() != null ? exam.getGradeScale() : GradeScale.NINE_GRADE;
            List<Integer> allScores = resultRepository.findByExamId(exam.getId()).stream()
                    .map(ExamResult::getTotalScore)
                    .toList();
            grade = gradeCalculator.calculateRelativeGrade(result.getTotalScore(), allScores, gradeScale);
        }

        return new ExamService.ManualGradeResult(
                result.getId(),
                exam.getId(),
                studentId,
                result.getTotalScore(),
                exam.calculateTotalPossibleScore(),
                grade,
                exam.isPassFail() ? result.isPassed() : null,
                questionResults
        );
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return java.util.UUID.randomUUID().toString().substring(0, 8);
        }
        return fileName.replaceAll("[^a-zA-Z0-9가-힣._-]", "_");
    }

}
