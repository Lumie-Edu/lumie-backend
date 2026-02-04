package com.lumie.exam.application.dto.response.statistics;

public record AcademyComparisonResponse(
        Long academyId,
        String academyName,
        int participantCount,
        double average,
        int grade1Count,
        double grade1Percentage
) {}
