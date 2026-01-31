package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.response.ExamResultResponse;
import com.lumie.exam.application.dto.response.QuestionResultResponse;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamResultRepository;
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
                .orElseThrow(() -> new ExamException(ExamErrorCode.RESULT_NOT_FOUND));

        return result.getQuestionResults().stream()
                .map(QuestionResultResponse::from)
                .toList();
    }
}
