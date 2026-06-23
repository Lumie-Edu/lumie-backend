package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.ExamQuestionType;

import java.time.Instant;

public record ExamQuestionTypeResponse(
        Long id,
        String name,
        Integer displayOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExamQuestionTypeResponse from(ExamQuestionType questionType) {
        return new ExamQuestionTypeResponse(
                questionType.getId(),
                questionType.getName(),
                questionType.getDisplayOrder(),
                questionType.getCreatedAt(),
                questionType.getUpdatedAt()
        );
    }
}
