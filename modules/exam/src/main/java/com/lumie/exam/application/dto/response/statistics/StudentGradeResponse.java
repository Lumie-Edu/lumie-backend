package com.lumie.exam.application.dto.response.statistics;

import com.lumie.exam.domain.vo.ExamCategory;

import java.time.Instant;
import com.lumie.common.util.Timestamps;

public record StudentGradeResponse(
        Long studentId,
        String phoneNumber,
        boolean isRegistered,
        String studentName,
        int score,
        int rank,
        double percentile,
        Integer grade,
        ExamCategory examCategory,
        boolean isPassed,
        Instant submittedAt,
        Long resultId
) {}
