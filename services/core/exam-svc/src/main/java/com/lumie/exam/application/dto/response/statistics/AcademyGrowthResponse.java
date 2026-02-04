package com.lumie.exam.application.dto.response.statistics;

import java.util.List;

public record AcademyGrowthResponse(
        String academyId,
        String academyName,
        int studentCount,
        double averageGrowthRate,
        List<StudentGrowth> topGrowthStudents,
        List<ExamGrowth> examGrowthTrend
) {
    public record StudentGrowth(
            Long studentId,
            String studentName,
            double growthRate,
            int examCount
    ) {}

    public record ExamGrowth(
            Long examId,
            String examName,
            java.time.LocalDateTime examDate,
            double academyAverage,
            double previousAcademyAverage,
            double growthRate
    ) {}
}
