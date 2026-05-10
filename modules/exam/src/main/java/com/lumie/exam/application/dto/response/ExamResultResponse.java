package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.ExamResult;

import java.time.Instant;
import com.lumie.common.util.Timestamps;

public record ExamResultResponse(
        Long id,
        Long examId,
        String examName,
        Long studentId,
        String phoneNumber,
        boolean isRegistered,
        Integer score,
        Integer grade,
        Integer correctCount,
        Integer incorrectCount,
        boolean hasOmrImage,
        Instant createdAt
) {
    public static ExamResultResponse from(ExamResult result) {
        return new ExamResultResponse(
                result.getId(),
                result.getExam().getId(),
                result.getExam().getName(),
                result.getStudentId(),
                result.getPhoneNumber(),
                result.getStudentId() != null,
                result.getTotalScore(),
                result.getGrade(),
                result.getCorrectCount(),
                result.getIncorrectCount(),
                result.getOmrImageKey() != null,
                Timestamps.utcInstant(result.getCreatedAt())
        );
    }
}
