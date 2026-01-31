package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.request.BulkSubmitResultsRequest;
import com.lumie.exam.application.dto.request.SubmitResultRequest;
import com.lumie.exam.application.dto.response.ExamResultResponse;
import com.lumie.exam.application.port.out.BillingServicePort;
import com.lumie.exam.application.port.out.ExamEventPublisherPort;
import com.lumie.exam.application.port.out.OmrServicePort;
import com.lumie.exam.application.port.out.StudentServicePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.service.GradeCalculator;
import com.lumie.exam.infrastructure.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResultCommandService {

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final GradeCalculator gradeCalculator;
    private final StudentServicePort studentService;
    private final BillingServicePort billingService;
    private final OmrServicePort omrService;
    private final ExamEventPublisherPort eventPublisher;

    @Transactional
    public List<ExamResultResponse> submitResults(Long examId, BulkSubmitResultsRequest request) {
        log.info("Submitting {} results for exam: {}", request.results().size(), examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        List<ExamResult> savedResults = new ArrayList<>();

        for (SubmitResultRequest resultReq : request.results()) {
            ExamResult result = processSubmitResult(exam, resultReq);
            savedResults.add(result);
        }

        gradeCalculator.calculateAndAssignGrades(exam, savedResults);
        List<ExamResult> finalResults = resultRepository.saveAll(savedResults);

        String tenantSlug = TenantContextHolder.getTenantSlug();
        for (ExamResult result : finalResults) {
            eventPublisher.publishResultSubmitted(result, tenantSlug);
        }

        log.info("Submitted {} results for exam: {}", finalResults.size(), examId);
        return finalResults.stream()
                .map(ExamResultResponse::from)
                .toList();
    }

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

        List<ExamResult> allResults = resultRepository.findByExamId(examId);
        allResults.add(result);
        gradeCalculator.calculateAndAssignGrades(exam, allResults);

        ExamResult savedResult = resultRepository.save(result);

        String tenantSlug = TenantContextHolder.getTenantSlug();
        eventPublisher.publishResultSubmitted(savedResult, tenantSlug);

        log.info("OMR grading completed for exam: {}, student: {}", examId, studentId);
        return ExamResultResponse.from(savedResult);
    }

    private ExamResult processSubmitResult(Exam exam, SubmitResultRequest request) {
        if (resultRepository.existsByExamIdAndStudentId(exam.getId(), request.studentId())) {
            throw new ExamException(ExamErrorCode.DUPLICATE_RESULT);
        }

        int totalScore = 0;
        List<QuestionResult> questionResults = new ArrayList<>();

        for (Map.Entry<String, String> entry : request.answers().entrySet()) {
            int questionNumber = Integer.parseInt(entry.getKey());
            String selectedChoice = entry.getValue();
            String correctAnswer = exam.getCorrectAnswerForQuestion(questionNumber);
            int maxScore = exam.getScoreForQuestion(questionNumber);

            QuestionResult qr = QuestionResult.create(
                    questionNumber,
                    selectedChoice,
                    correctAnswer,
                    maxScore
            );
            questionResults.add(qr);

            if (qr.isCorrect()) {
                totalScore += maxScore;
            }
        }

        ExamResult result = ExamResult.create(exam, request.studentId(), totalScore);
        for (QuestionResult qr : questionResults) {
            result.addQuestionResult(qr);
        }

        return result;
    }
}
