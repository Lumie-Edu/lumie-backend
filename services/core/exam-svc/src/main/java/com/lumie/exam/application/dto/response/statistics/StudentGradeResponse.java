package com.lumie.exam.application.dto.response.statistics;

import com.lumie.exam.domain.vo.ExamCategory;

import java.time.LocalDateTime;

public record StudentGradeResponse(
        Long studentId,
        String studentName,
        String studentPhone,
        int score,
        int rank,
        double percentile,
        Integer grade,
        ExamCategory examCategory,
        boolean isPassed,
        LocalDateTime submittedAt
) {}
