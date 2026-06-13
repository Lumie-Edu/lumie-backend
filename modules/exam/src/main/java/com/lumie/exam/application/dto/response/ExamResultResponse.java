package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.ExamResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record ExamResultResponse(
        Long id,
        Long examId,
        String examName,
        @Schema(nullable = true) Long studentId,
        @Schema(nullable = true) String studentName,
        @Schema(nullable = true) String phoneNumber,
        boolean isRegistered,
        Integer score,
        Integer totalPossibleScore,
        @Schema(nullable = true) Integer grade,
        Integer correctCount,
        Integer incorrectCount,
        boolean hasOmrImage,
        Instant createdAt
) {
    public static ExamResultResponse from(ExamResult result) {
        return from(result, null);
    }

    public static ExamResultResponse from(ExamResult result, String studentName) {
        boolean studentDeleted = result.isStudentDeleted();
        Long studentId = studentDeleted ? null : result.getStudentId();
        return new ExamResultResponse(
                result.getId(),
                result.getExam().getId(),
                result.getExam().getName(),
                studentId,
                studentName,
                studentDeleted ? null : result.getPhoneNumber(),
                studentId != null,
                result.getTotalScore(),
                result.getExam().calculateTotalPossibleScore(),
                result.getGrade(),
                result.getCorrectCount(),
                result.getIncorrectCount(),
                result.getOmrImageKey() != null,
                result.getCreatedAt()
        );
    }
}
