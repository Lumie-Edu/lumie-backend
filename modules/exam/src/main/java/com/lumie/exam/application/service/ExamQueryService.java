package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.response.ExamFullResponse;
import com.lumie.exam.application.dto.response.ExamResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.vo.ExamSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ExamQueryService {

    private final ExamRepository examRepository;

    public ExamResponse getExam(Long examId) {
        log.debug("Getting exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));

        return ExamResponse.from(exam);
    }

    public ExamFullResponse getExamFull(Long examId) {
        log.debug("Getting exam full (admin): {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));

        return ExamFullResponse.from(exam);
    }

    public Page<ExamResponse> listExams(Pageable pageable) {
        log.debug("Listing all exams with pagination");

        return examRepository.findBySource(ExamSource.EXAM_MANAGEMENT, pageable)
                .map(ExamResponse::from);
    }
}
