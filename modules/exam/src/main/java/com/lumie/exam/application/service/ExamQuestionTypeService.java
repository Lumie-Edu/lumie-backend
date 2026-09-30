package com.lumie.exam.application.service;

import com.lumie.common.auth.AuthorizationGuard;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.application.dto.request.CreateExamQuestionTypeRequest;
import com.lumie.exam.application.dto.response.ExamQuestionTypeResponse;
import com.lumie.exam.application.port.out.ExamQuestionTypePort;
import com.lumie.exam.domain.entity.ExamQuestionType;
import com.lumie.exam.domain.exception.ExamErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamQuestionTypeService {

    private static final List<String> DEFAULT_QUESTION_TYPES = List.of(
            "듣기",
            "기초유형",
            "내용파악",
            "빈칸",
            "순서",
            "삽입",
            "어법",
            "어휘",
            "단어"
    );

    private final ExamQuestionTypePort examQuestionTypePort;

    @Transactional(readOnly = true)
    public List<ExamQuestionTypeResponse> listQuestionTypes() {
        return examQuestionTypePort.findAllOrdered().stream()
                .map(ExamQuestionTypeResponse::from)
                .toList();
    }

    @Transactional
    public ExamQuestionTypeResponse createQuestionType(CreateExamQuestionTypeRequest request) {
        AuthorizationGuard.requireStaff();

        String name = request.name().trim();
        if (examQuestionTypePort.existsByName(name)) {
            throw new BusinessException(ExamErrorCode.DUPLICATE_QUESTION_TYPE);
        }

        int nextOrder = examQuestionTypePort.findMaxDisplayOrder() + 1;
        ExamQuestionType questionType = ExamQuestionType.create(name, nextOrder);
        ExamQuestionType saved;
        try {
            saved = examQuestionTypePort.save(questionType);
        } catch (DataIntegrityViolationException e) {
            if (!isUniqueViolation(e)) {
                throw e;
            }
            throw new BusinessException(ExamErrorCode.DUPLICATE_QUESTION_TYPE);
        }

        log.info("Exam question type created: {}", saved.getId());
        return ExamQuestionTypeResponse.from(saved);
    }

    @Transactional
    public void deleteQuestionType(Long questionTypeId) {
        AuthorizationGuard.requireStaff();

        if (!examQuestionTypePort.deleteById(questionTypeId)) {
            throw new BusinessException(ExamErrorCode.QUESTION_TYPE_NOT_FOUND);
        }
        log.info("Exam question type deleted: {}", questionTypeId);
    }

    @Transactional
    public void seedDefaultQuestionTypesIfEmpty() {
        List<ExamQuestionType> existingDefaults = examQuestionTypePort.findAllByNameIn(DEFAULT_QUESTION_TYPES);
        if (!existingDefaults.isEmpty()) {
            return;
        }

        List<ExamQuestionType> defaults = IntStream.range(0, DEFAULT_QUESTION_TYPES.size())
                .mapToObj(index -> ExamQuestionType.create(DEFAULT_QUESTION_TYPES.get(index), index + 1))
                .toList();

        examQuestionTypePort.saveAll(defaults);
        log.info("Seeded {} default exam question types", defaults.size());
    }



    private boolean isUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        return cause instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState());
    }
}
