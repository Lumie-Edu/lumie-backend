package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;

import java.time.LocalDateTime;

public record ExamResponse(
        Long id,
        String name,
        ExamCategory category,
        GradingType gradingType,
        GradeScale gradeScale,
        Integer totalQuestions,
        Integer totalPossibleScore,
        Integer passScore,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ExamResponse from(Exam exam) {
        return new ExamResponse(
                exam.getId(),
                exam.getName(),
                exam.getCategory(),
                exam.getGradingType(),
                exam.getGradeScale(),
                exam.getTotalQuestions(),
                exam.calculateTotalPossibleScore(),
                exam.getPassScore(),
                exam.getCreatedAt(),
                exam.getUpdatedAt()
        );
    }
}
