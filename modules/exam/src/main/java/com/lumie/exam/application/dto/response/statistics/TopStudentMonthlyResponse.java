package com.lumie.exam.application.dto.response.statistics;

public record TopStudentMonthlyResponse(
        Long studentId,
        String studentName,
        Double avgScore,
        Integer attemptCount
) {}
