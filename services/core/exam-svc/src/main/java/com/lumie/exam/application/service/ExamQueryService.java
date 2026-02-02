package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.response.ExamDetailResponse;
import com.lumie.exam.application.dto.response.ExamResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamRepository;
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
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        return ExamResponse.from(exam);
    }

    public ExamDetailResponse getExamDetail(Long examId) {
        log.debug("Getting exam detail (admin): {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        return ExamDetailResponse.from(exam);
    }

    public Page<ExamResponse> listExams(Pageable pageable) {
        log.debug("Listing all exams with pagination");

        return examRepository.findAll(pageable)
                .map(ExamResponse::from);
    }
}
