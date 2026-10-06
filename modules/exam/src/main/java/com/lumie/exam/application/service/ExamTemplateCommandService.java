package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.request.CreateExamTemplateRequest;
import com.lumie.exam.application.dto.request.UpdateExamTemplateRequest;
import com.lumie.exam.application.dto.response.ExamTemplateResponse;
import com.lumie.exam.domain.entity.ExamTemplate;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.repository.ExamTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

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
                request.gradingType(),
                request.gradeScale(),
                request.totalQuestions(),
                request.questionScores(),
                request.questionTypes(),
                request.passScore()
        );
        validateConfiguration(template.getCategory(), template.getGradingType(), template.getGradeScale(),
                template.getPassScore(), template.getQuestionScores());

        ExamTemplate saved = examTemplateRepository.save(template);

        log.info("Exam template created with id: {}", saved.getId());
        return ExamTemplateResponse.from(saved);
    }

    @Transactional
    public ExamTemplateResponse update(Long id, UpdateExamTemplateRequest request) {
        log.info("Updating exam template: {}", id);

        ExamTemplate template = examTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.TEMPLATE_NOT_FOUND));

        if (request.category() != null || request.gradingType() != null
                || request.gradeScale() != null || request.passScore() != null
                || request.questionScores() != null) {
            ExamCategory category = request.category() != null ? request.category() : template.getCategory();
            if (category == ExamCategory.PASS_FAIL
                    && (request.gradingType() != null || request.gradeScale() != null)) {
                throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA,
                        "PASS_FAIL templates do not use academic grading settings");
            }
            GradingType type = request.gradingType() != null ? request.gradingType() : template.getGradingType();
            GradeScale scale = request.gradeScale() != null ? request.gradeScale() : template.getGradeScale();
            Integer cutoff = request.passScore() != null ? request.passScore() : template.getPassScore();
            Map<String, Integer> scores = request.questionScores() != null
                    ? request.questionScores() : template.getQuestionScores();
            validateConfiguration(category, type, scale, cutoff, scores);
        }

        template.update(
                request.name(),
                request.category(),
                request.gradingType(),
                request.gradeScale(),
                request.totalQuestions(),
                request.questionScores(),
                request.questionTypes(),
                request.passScore()
        );

        ExamTemplate updated = examTemplateRepository.save(template);

        log.info("Exam template updated: {}", updated.getId());
        return ExamTemplateResponse.from(updated);
    }

    private static void validateConfiguration(ExamCategory category, GradingType type,
                                              GradeScale scale, Integer passScore,
                                              Map<String, Integer> questionScores) {
        ExamCategory effectiveCategory = category != null ? category : ExamCategory.GRADED;
        if (effectiveCategory == ExamCategory.GRADED && (type == null || scale == null)) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA,
                    "Graded templates require a grading type and scale");
        }
        int totalPossibleScore = questionScores == null ? 0
                : questionScores.values().stream().mapToInt(Integer::intValue).sum();
        if (effectiveCategory == ExamCategory.PASS_FAIL
                && (passScore == null || passScore < 1 || passScore > totalPossibleScore)) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA,
                    "PASS_FAIL templates require a pass score within the possible score range");
        }
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting exam template: {}", id);

        if (!examTemplateRepository.existsById(id)) {
            throw new BusinessException(ExamErrorCode.TEMPLATE_NOT_FOUND);
        }

        examTemplateRepository.deleteById(id);
        log.info("Exam template deleted: {}", id);
    }
}
