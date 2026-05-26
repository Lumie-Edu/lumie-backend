package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.response.ExamResultResponse;
import com.lumie.exam.application.dto.response.OmrJobStatusResponse;
import com.lumie.exam.application.dto.response.QuestionResultResponse;
import com.lumie.exam.application.port.out.OmrStoragePort;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.repository.OmrGradingJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ResultQueryService {

    private final ExamResultRepository resultRepository;
    private final OmrGradingJobRepository jobRepository;
    private final OmrStoragePort storagePort;

    public List<ExamResultResponse> getExamResults(Long examId) {
        log.debug("Getting results for exam: {}", examId);

        return resultRepository.findByExamId(examId).stream()
                .map(ExamResultResponse::from)
                .toList();
    }

    public List<ExamResultResponse> getStudentResults(Long studentId) {
        log.debug("Getting results for student: {}", studentId);

        return resultRepository.findByStudentId(studentId).stream()
                .map(ExamResultResponse::from)
                .toList();
    }

    public List<QuestionResultResponse> getQuestionResults(Long resultId) {
        log.debug("Getting question results for result: {}", resultId);

        ExamResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));

        var exam = result.getExam();
        return result.getQuestionResults().stream()
                .map(qr -> QuestionResultResponse.from(qr, exam))
                .toList();
    }

    public OmrJobStatusResponse getOmrJobStatus(Long examId, Long jobId) {
        log.debug("Getting OMR job status: examId={}, jobId={}", examId, jobId);

        return jobRepository.findById(jobId)
                .map(OmrJobStatusResponse::from)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));
    }

    public List<OmrJobStatusResponse> getOmrJobs(Long examId) {
        log.debug("Getting OMR jobs for exam: {}", examId);

        return jobRepository.findByExamIdOrderByCreatedAtDesc(examId).stream()
                .map(OmrJobStatusResponse::from)
                .toList();
    }

    public byte[] getOmrImageData(Long resultId) {
        ExamResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.RESULT_NOT_FOUND));

        if (result.getOmrImageKey() == null) {
            throw new BusinessException(ExamErrorCode.OMR_IMAGE_NOT_FOUND);
        }

        return storagePort.getObject(result.getOmrImageKey());
    }
}
