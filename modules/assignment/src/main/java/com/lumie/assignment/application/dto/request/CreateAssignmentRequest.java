package com.lumie.assignment.application.dto.request;

import jakarta.validation.Valid;

import java.time.Instant;
import java.util.List;

public record CreateAssignmentRequest(
    Long classId,

    Instant dueDate,
    Integer maxScore,
    List<Long> attachments,
    String status,
    String submissionMode,
    String evaluationMode,
    @Valid CreateAssignmentExamRequest exam,
    Long linkedExamId,
    List<Long> targetClassIds,
    List<Long> targetStudentIds,
    Boolean showScore,
    Boolean showPassFail,
    Boolean showGrade,
    Boolean showQuestionResults,
    Boolean showCorrectAnswers
) {
    public CreateAssignmentRequest(Long classId, Instant dueDate, Integer maxScore, List<Long> attachments) {
        this(classId, dueDate, maxScore, attachments,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
