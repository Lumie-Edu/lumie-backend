package com.lumie.exam.application.dto.request;

import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;

import java.util.Map;

public record UpdateExamRequest(
        String name,
        ExamCategory category,
        GradingType gradingType,
        GradeScale gradeScale,
        Integer totalQuestions,
        Map<String, String> correctAnswers,
        Map<String, Integer> questionScores,
        Map<String, String> questionTypes,
        Integer passScore
) {
}
