package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.request.CreateExamTemplateRequest;
import com.lumie.exam.application.dto.request.UpdateExamTemplateRequest;
import com.lumie.exam.application.dto.response.ExamTemplateResponse;
import com.lumie.exam.domain.entity.ExamTemplate;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamTemplateCommandService {

    private final ExamTemplateRepository examTemplateRepository;

    @Transactional
    public ExamTemplateResponse create(CreateExamTemplateRequest request) {
        log.info("Creating exam template: {}", request.name());

        ExamTemplate template = ExamTemplate.create(
                request.name(),
                request.category(),
                request.totalQuestions(),
                request.questionScores(),
                request.questionTypes(),
                request.passScore()
        );

        ExamTemplate saved = examTemplateRepository.save(template);

        log.info("Exam template created with id: {}", saved.getId());
        return ExamTemplateResponse.from(saved);
    }

    @Transactional
    public ExamTemplateResponse update(Long id, UpdateExamTemplateRequest request) {
        log.info("Updating exam template: {}", id);

        ExamTemplate template = examTemplateRepository.findById(id)
                .orElseThrow(() -> new ExamException(ExamErrorCode.TEMPLATE_NOT_FOUND));

        template.update(
                request.name(),
                request.category(),
                request.totalQuestions(),
                request.questionScores(),
                request.questionTypes(),
                request.passScore()
        );

        ExamTemplate updated = examTemplateRepository.save(template);

        log.info("Exam template updated: {}", updated.getId());
        return ExamTemplateResponse.from(updated);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting exam template: {}", id);

        if (!examTemplateRepository.existsById(id)) {
            throw new ExamException(ExamErrorCode.TEMPLATE_NOT_FOUND);
        }

        examTemplateRepository.deleteById(id);
        log.info("Exam template deleted: {}", id);
    }
}
