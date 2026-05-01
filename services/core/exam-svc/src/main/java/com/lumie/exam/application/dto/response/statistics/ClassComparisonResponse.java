package com.lumie.exam.application.dto.response.statistics;

import com.lumie.exam.domain.vo.ExamCategory;

import java.util.List;

public record ClassComparisonResponse(
        Long examId,
        String examName,
        ExamCategory examCategory,
        int totalParticipants,
        List<ClassMetric> classes
) {
    public record ClassMetric(
            Long classId,            // null = 미배정
            String className,
            int participantCount,
            double mean,
            double stddev,
            int highest,
            int lowest,
            Double grade1Ratio,      // null when PASS_FAIL
            Double passRate,         // null when GRADED
            List<GradeBucket> gradeDistribution
    ) {}

    public record GradeBucket(
            int grade,
            int count,
            double percentage
    ) {}
}
