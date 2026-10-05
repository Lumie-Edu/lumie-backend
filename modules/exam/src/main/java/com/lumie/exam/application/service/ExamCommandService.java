package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.request.CreateExamRequest;
import com.lumie.exam.application.dto.request.UpdateExamRequest;
import com.lumie.exam.application.dto.response.ExamResponse;
import com.lumie.exam.api.ExamResultChangedEvent;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.ExamSource;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamCommandService {

    private final ExamRepository examRepository;
    private final ExamResultRepository resultRepository;
    private final GradeCalculator gradeCalculator;
    private final ApplicationEventPublisher eventPublisher;

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
        requirePassScore(exam.getCategory(), exam.getPassScore(), exam.calculateTotalPossibleScore());
        return exam;
    }

    private Exam createAssignmentExamEntity(CreateExamRequest request) {
        Exam exam = Exam.createForAssignment(
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
        requirePassScore(exam.getCategory(), exam.getPassScore(), exam.calculateTotalPossibleScore());
        return exam;
    }

    @Transactional
    public ExamResponse updateExam(Long examId, UpdateExamRequest request) {
        log.info("Updating exam: {}", examId);

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ExamErrorCode.EXAM_NOT_FOUND));

        List<ExamResult> existingResults = resultRepository.findByExamId(examId);
        if (!existingResults.isEmpty() && (
                changed(request.totalQuestions(), exam.getTotalQuestions())
                || changed(request.correctAnswers(), exam.getCorrectAnswers())
                || changed(request.questionScores(), exam.getQuestionScores())
                || changed(request.questionTypes(), exam.getQuestionTypes()))) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA,
                    "Cannot change answer keys, question scores, types or count after results exist");
        }

        ExamCategory effectiveCategory = request.category() != null ? request.category() : exam.getCategory();
        if (effectiveCategory == ExamCategory.PASS_FAIL) {
            Integer effectivePassScore = request.passScore() != null ? request.passScore() : exam.getPassScore();
            int effectiveMaxScore = request.questionScores() != null
                    ? request.questionScores().values().stream().mapToInt(Integer::intValue).sum()
                    : exam.calculateTotalPossibleScore();
            requirePassScore(ExamCategory.PASS_FAIL, effectivePassScore, effectiveMaxScore);
        }

        boolean gradingConfigurationChanged = changed(request.gradingType(), exam.getGradingType())
                || changed(request.gradeScale(), exam.getGradeScale())
                || changed(request.category(), exam.getCategory())
                || changed(request.passScore(), exam.getPassScore());
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
        if (gradingConfigurationChanged) {
            List<Integer> scores = existingResults.stream().map(result -> result.getTotalScore()).toList();
            for (var result : existingResults) {
                gradeCalculator.reconcileStoredGrade(updatedExam, result);
                if (updatedExam.getSource() != ExamSource.ASSIGNMENT) {
                    continue;
                }
                Map<String, String> answers = new LinkedHashMap<>();
                result.getQuestionResults().forEach(question -> answers.put(
                        String.valueOf(question.getQuestionNumber()), question.getSelectedChoice()));
                eventPublisher.publishEvent(new ExamResultChangedEvent(
                        TenantContextHolder.getTenantId(), examId, result.getId(), answers,
                        result.getTotalScore(), updatedExam.isPassFail() ? result.passStatus() : null,
                        gradeCalculator.resolveStoredOrRelativeGrade(updatedExam, result, scores)));
            }
        }

        log.info("Exam updated: {}", updatedExam.getId());
        return ExamResponse.from(updatedExam);
    }

    private static boolean changed(Object requested, Object current) {
        return requested != null && !Objects.equals(requested, current);
    }

    private static void requirePassScore(ExamCategory category, Integer passScore, int totalPossibleScore) {
        if (category == ExamCategory.PASS_FAIL
                && (passScore == null || passScore < 1 || passScore > totalPossibleScore)) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA,
                    "PASS_FAIL exams require a pass score within the possible score range");
        }
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
