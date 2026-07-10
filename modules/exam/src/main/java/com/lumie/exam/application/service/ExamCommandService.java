package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.request.CreateExamRequest;
import com.lumie.exam.application.dto.request.UpdateExamRequest;
import com.lumie.exam.application.dto.response.ExamResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamCommandService {

    private final ExamRepository examRepository;

    @Transactional
    public ExamResponse createExam(CreateExamRequest request) {
        log.info("Creating exam: {}", request.name());

        Exam savedExam = examRepository.save(createExamEntity(request));

        log.info("Exam created with id: {}", savedExam.getId());
        return ExamResponse.from(savedExam);
    }

    @Transactional
    public Long createAssignmentExam(CreateExamRequest request) {
        log.info("Creating assignment-private exam: {}", request.name());

        Exam savedExam = examRepository.save(createAssignmentExamEntity(request));

        log.info("Assignment-private exam created with id: {}", savedExam.getId());
        return savedExam.getId();
    }

    private Exam createExamEntity(CreateExamRequest request) {
        return Exam.create(
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
    }

    private Exam createAssignmentExamEntity(CreateExamRequest request) {
        return Exam.createForAssignment(
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
    }

    @Transactional
    public ExamResponse updateExam(Long examId, UpdateExamRequest request) {
        log.info("Updating exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));

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
            throw new BusinessException(ExamErrorCode.EXAM_NOT_FOUND);
        }

        examRepository.deleteById(examId);
        log.info("Exam deleted: {}", examId);
    }
}
