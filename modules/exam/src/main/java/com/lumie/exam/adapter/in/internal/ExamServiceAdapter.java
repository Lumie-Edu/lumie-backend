package com.lumie.exam.adapter.in.internal;

import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.api.ExamService;
import com.lumie.exam.application.dto.request.CreateExamRequest;
import com.lumie.exam.application.service.ExamCommandService;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.application.service.ResultCommandService;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import com.lumie.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExamServiceAdapter implements ExamService {

    private final ExamResultRepository examResultRepository;
    private final ExamRepository examRepository;
    private final ResultCommandService resultCommandService;
    private final ExamCommandService examCommandService;

    @Override
    @Transactional
    public Long createExamForAssignment(String tenantSlug, CreateExamData request) {
        return withinTenant(tenantSlug, () -> examCommandService.createAssignmentExam(toCreateExamRequest(request)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExamAssignmentData> getExamForAssignment(String tenantSlug, Long examId) {
        return withinTenant(tenantSlug, () -> {
            if (examId == null) {
                return Optional.empty();
            }
            return examRepository.findById(examId)
                    .map(this::toAssignmentData);
        });
    }

    @Override
    public ManualGradeResult gradeManualAnswers(String tenantSlug, Long examId, Long studentId, Map<String, String> answers) {
        return withinTenant(tenantSlug, () -> resultCommandService.gradeManualAnswers(examId, studentId, answers));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionGradeData> getQuestionResultsForAssignment(String tenantSlug, Long examResultId) {
        return withinTenant(tenantSlug, () -> {
            if (examResultId == null) {
                return List.of();
            }
            return examResultRepository.findWithQuestionResultsById(examResultId)
                    .map(result -> result.getQuestionResults().stream()
                            .sorted(Comparator.comparing(com.lumie.exam.domain.entity.QuestionResult::getQuestionNumber))
                            .map(questionResult -> {
                                Integer questionNumber = questionResult.getQuestionNumber();
                                Exam exam = result.getExam();
                                return new QuestionGradeData(
                                        questionResult.getId(),
                                        questionNumber,
                                        questionResult.getSelectedChoice(),
                                        questionResult.isCorrect(),
                                        questionResult.getScore(),
                                        questionResult.getEarnedScore(),
                                        exam.getCorrectAnswerForQuestion(questionNumber),
                                        exam.getQuestionType(questionNumber)
                                );
                            })
                            .toList())
                    .orElseGet(List::of);
        });
    }

    private <T> T withinTenant(String tenantSlug, Supplier<T> action) {
        String currentTenant = TenantContextHolder.getTenant();
        if (currentTenant != null && tenantSlug != null && !currentTenant.equals(tenantSlug)) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA,
                    "Tenant context mismatch for exam assignment API");
        }
        return TenantContextHolder.withinContext(tenantSlug, TenantContextHolder.getTenantId(), action);
    }

    private CreateExamRequest toCreateExamRequest(CreateExamData request) {
        return new CreateExamRequest(
                request.name(),
                parseCategory(request.category()),
                parseGradingType(request.gradingType()),
                parseGradeScale(request.gradeScale()),
                request.totalQuestions(),
                request.correctAnswers(),
                request.questionScores(),
                request.questionTypes(),
                request.passScore()
        );
    }

    private ExamAssignmentData toAssignmentData(Exam exam) {
        return new ExamAssignmentData(
                exam.getId(),
                exam.getName(),
                exam.getCategory().name(),
                exam.getGradingType().name(),
                exam.getGradeScale() != null ? exam.getGradeScale().name() : null,
                exam.getTotalQuestions(),
                exam.calculateTotalPossibleScore(),
                exam.getPassScore(),
                IntStream.rangeClosed(1, exam.getTotalQuestions())
                        .mapToObj(questionNumber -> new QuestionMetadata(
                                questionNumber,
                                exam.getScoreForQuestion(questionNumber),
                                exam.getQuestionType(questionNumber)
                        ))
                        .sorted(Comparator.comparing(QuestionMetadata::questionNumber))
                        .toList()
        );
    }

    private ExamCategory parseCategory(String category) {
        return parseEnum(category, ExamCategory.class, "category");
    }

    private GradingType parseGradingType(String gradingType) {
        return parseEnum(gradingType, GradingType.class, "gradingType");
    }

    private GradeScale parseGradeScale(String gradeScale) {
        return parseEnum(gradeScale, GradeScale.class, "gradeScale");
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumType, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ExamErrorCode.INVALID_EXAM_DATA,
                    "Invalid %s: %s".formatted(fieldName, value));
        }
    }
}
