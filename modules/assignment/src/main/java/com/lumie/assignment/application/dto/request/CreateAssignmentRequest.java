package com.lumie.assignment.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record CreateAssignmentRequest(
    @NotNull(message = "Assignment due date is required")
    Instant dueDate,
    Integer maxScore,
    List<Long> attachments,
    String status,
    String submissionMode,
    String evaluationMode,
    @NotNull @Valid CreateAssignmentExamRequest exam,
    List<Long> targetClassIds,
    List<Long> targetStudentIds,
    Boolean showScore,
    Boolean showPassFail,
    Boolean showGrade,
    Boolean showQuestionResults,
    Boolean showCorrectAnswers
) {
    public CreateAssignmentRequest(Instant dueDate, Integer maxScore, List<Long> attachments) {
        this(dueDate, maxScore, attachments, null, null, null, null, null, null,
                null, null, null, null, null);
    }
}
