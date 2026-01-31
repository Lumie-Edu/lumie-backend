package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.ExamResult;

import java.time.LocalDateTime;

public record ExamResultResponse(
        Long id,
        Long examId,
        String examName,
        Long studentId,
        Integer totalScore,
        Integer grade,
        Integer correctCount,
        Integer incorrectCount,
        LocalDateTime createdAt
) {
    public static ExamResultResponse from(ExamResult result) {
        return new ExamResultResponse(
                result.getId(),
                result.getExam().getId(),
                result.getExam().getName(),
                result.getStudentId(),
                result.getTotalScore(),
                result.getGrade(),
                result.getCorrectCount(),
                result.getIncorrectCount(),
                result.getCreatedAt()
        );
    }
}
