package com.lumie.exam.application.dto.request;

import com.lumie.exam.domain.vo.ExamCategory;

import java.util.Map;

public record UpdateExamTemplateRequest(
        String name,
        ExamCategory category,
        Integer totalQuestions,
        Map<String, Integer> questionScores,
        Map<String, String> questionTypes,
        Integer passScore
) {
}
