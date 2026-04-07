package com.lumie.exam.application.service;

import com.lumie.exam.application.port.out.BillingServicePort;
import com.lumie.exam.application.port.out.OmrServicePort;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.application.port.out.StudentServicePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import lombok.RequiredArgsConstructor;
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
    private final OmrServicePort omrService;
    private final OmrStoragePort storagePort;
    private final StudentServicePort studentService;
    private final BillingServicePort billingService;

    @Transactional
    public Map<String, Object> processSingleImage(
            Long jobId, Long examId, String imageKey, String tenantSlug) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

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

            ExamResult examResult;
            if (studentOpt.isPresent()) {
                StudentServicePort.StudentInfo student = studentOpt.get();
                examResult = resultCommandService.resolveResult(exam, student.id(), omrResult);
                resultMap.put("studentId", student.id());
                resultMap.put("studentName", student.name());
            } else {
                examResult = resultCommandService.resolveResultForUnregistered(exam, fullPhone, omrResult);
            }

            String tenantId = extractTenantId(tenantSlug);
            String imageId = studentOpt.map(s -> String.valueOf(s.id())).orElse(fullPhone);
            String permanentKey = "omr/results/" + tenantId + "/" + examId + "/" + imageId;
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
            resultMap.put("totalScore", omrResult.totalScore());
            resultMap.put("grade", omrResult.grade());
            resultMap.put("resultId", savedResult.getId());

        } catch (Exception e) {
            log.error("Failed to process OMR image: {}", imageKey, e);
            resultMap.put("fileName", fileName);
            resultMap.put("success", false);
            resultMap.put("saved", false);
            resultMap.put("error", e.getMessage());
        }

        return resultMap;
    }

    private String extractTenantId(String tenantSlug) {
        if (tenantSlug.startsWith("inst-")) {
            return tenantSlug.substring(5);
        }
        return tenantSlug;
    }

    private String extractFileName(String objectKey) {
        int lastSlash = objectKey.lastIndexOf('/');
        return lastSlash >= 0 ? objectKey.substring(lastSlash + 1) : objectKey;
    }
}
