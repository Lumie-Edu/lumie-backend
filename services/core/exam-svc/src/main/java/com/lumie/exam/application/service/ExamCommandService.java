package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.request.CreateExamRequest;
import com.lumie.exam.application.dto.request.UpdateExamRequest;
import com.lumie.exam.application.dto.response.ExamResponse;
import com.lumie.exam.application.port.out.ExamEventPublisherPort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamCommandService {

    private final ExamRepository examRepository;
    private final ExamEventPublisherPort eventPublisher;

    @Transactional
    public ExamResponse createExam(CreateExamRequest request) {
        log.info("Creating exam: {}", request.name());

        Exam exam = Exam.create(
                request.name(),
                request.category(),
                request.gradingType(),
                request.gradeScale(),
                request.totalQuestions(),
                request.correctAnswers(),
                request.questionScores(),
                request.questionTypes(),
                request.passScore()
        );

        Exam savedExam = examRepository.save(exam);

        String tenantSlug = TenantContextHolder.getTenantSlug();
        eventPublisher.publishExamCreated(savedExam, tenantSlug);

        log.info("Exam created with id: {}", savedExam.getId());
        return ExamResponse.from(savedExam);
    }

    @Transactional
    public ExamResponse updateExam(Long examId, UpdateExamRequest request) {
        log.info("Updating exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ExamException(ExamErrorCode.EXAM_NOT_FOUND));

        exam.update(
                request.name(),
                request.category(),
                request.gradingType(),
                request.gradeScale(),
                request.totalQuestions(),
                request.correctAnswers(),
                request.questionScores(),
                request.questionTypes(),
                request.passScore()
        );

        Exam updatedExam = examRepository.save(exam);

        log.info("Exam updated: {}", updatedExam.getId());
        return ExamResponse.from(updatedExam);
    }

    @Transactional
    public void deleteExam(Long examId) {
        log.info("Deleting exam: {}", examId);

        if (!examRepository.existsById(examId)) {
            throw new ExamException(ExamErrorCode.EXAM_NOT_FOUND);
        }

        examRepository.deleteById(examId);
        log.info("Exam deleted: {}", examId);
    }
}
