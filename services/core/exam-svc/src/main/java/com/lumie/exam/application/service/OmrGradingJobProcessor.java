package com.lumie.exam.application.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OmrGradingJobProcessor {

    private final OmrGradingJobRepository jobRepository;
    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final OmrServicePort omrService;
    private final OmrStoragePort storagePort;
    private final StudentServicePort studentService;
    private final BillingServicePort billingService;
    private final GradeCalculator gradeCalculator;

    @Transactional
    public void startProcessing(Long jobId) {
        OmrGradingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.RESULT_NOT_FOUND));
        job.startProcessing();
        jobRepository.save(job);
    }

    @Transactional
    public Map<String, Object> processImageAndUpdateProgress(
            Long jobId, Long examId, String imageKey, String tenantSlug) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        OmrGradingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.RESULT_NOT_FOUND));

        String fileName = extractFileName(imageKey);
        Map<String, Object> resultMap = new LinkedHashMap<>();

        try {
            byte[] imageData = storagePort.getObject(imageKey);

            OmrServicePort.OmrGradingResult omrResult = omrService.gradeOmrImage(
                    imageData,
                    exam.getCorrectAnswers(),
                    exam.getQuestionScores(),
                    exam.getQuestionTypes()
            );

            String fullPhone = "010" + omrResult.phoneNumber();
            Optional<StudentServicePort.StudentInfo> studentOpt = studentService.findByPhone(fullPhone);

            if (studentOpt.isEmpty()) {
                resultMap.put("fileName", fileName);
                resultMap.put("success", true);
                resultMap.put("saved", false);
                resultMap.put("phoneNumber", omrResult.phoneNumber());
                resultMap.put("totalScore", omrResult.totalScore());
                resultMap.put("grade", omrResult.grade());
                resultMap.put("error", "학생을 찾을 수 없음: " + fullPhone);
                job.incrementProcessed(true, false);
                jobRepository.save(job);
                return resultMap;
            }

            StudentServicePort.StudentInfo student = studentOpt.get();

            ExamResult examResult = ExamResult.create(exam, student.id(), omrResult.totalScore());
            for (OmrServicePort.OmrQuestionResult qr : omrResult.results()) {
                QuestionResult questionResult = QuestionResult.create(
                        qr.questionNumber(),
                        qr.studentAnswer(),
                        qr.correctAnswer(),
                        qr.score()
                );
                examResult.addQuestionResult(questionResult);
            }

            gradeCalculator.calculateAndAssignGradeIfAbsolute(exam, examResult);

            // Copy OMR image to permanent path
            String tenantId = extractTenantId(tenantSlug);
            String permanentKey = "omr/results/" + tenantId + "/" + examId + "/" + student.id();
            try {
                storagePort.putObject(permanentKey, imageData, "image/png");
                examResult.setOmrImageKey(permanentKey);
            } catch (Exception e) {
                log.warn("Failed to copy OMR image to permanent path: {}", permanentKey, e);
            }

            ExamResult savedResult = resultRepository.save(examResult);
            billingService.consumeOmrQuota();

            resultMap.put("fileName", fileName);
            resultMap.put("success", true);
            resultMap.put("saved", true);
            resultMap.put("phoneNumber", omrResult.phoneNumber());
            resultMap.put("studentId", student.id());
            resultMap.put("studentName", student.name());
            resultMap.put("totalScore", omrResult.totalScore());
            resultMap.put("grade", omrResult.grade());
            resultMap.put("resultId", savedResult.getId());
            job.incrementProcessed(true, true);

        } catch (Exception e) {
            log.error("Failed to process OMR image: {}", imageKey, e);
            resultMap.put("fileName", fileName);
            resultMap.put("success", false);
            resultMap.put("saved", false);
            resultMap.put("error", e.getMessage());
            job.incrementProcessed(false, false);
        }

        jobRepository.save(job);
        return resultMap;
    }

    private String extractTenantId(String tenantSlug) {
        if (tenantSlug.startsWith("inst-")) {
            return tenantSlug.substring(5);
        }
        return tenantSlug;
    }

    @Transactional
    public void completeJob(Long jobId, List<Map<String, Object>> results) {
        OmrGradingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.RESULT_NOT_FOUND));
        job.complete(results);
        jobRepository.save(job);
    }

    @Transactional
    public void failJob(Long jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.fail(null);
            jobRepository.save(job);
        });
    }

    private String extractFileName(String objectKey) {
        int lastSlash = objectKey.lastIndexOf('/');
        return lastSlash >= 0 ? objectKey.substring(lastSlash + 1) : objectKey;
    }
}
