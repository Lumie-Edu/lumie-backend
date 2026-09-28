package com.lumie.assignment.application.dto.request;

import java.time.Instant;
import java.util.List;

public record UpdateAssignmentRequest(
    Instant dueDate,
    Integer maxScore,
    List<Long> attachments,
    String status,
    String submissionMode,
    String evaluationMode,
    List<Long> targetClassIds,
    List<Long> targetStudentIds,
    Boolean showScore,
    Boolean showPassFail,
    Boolean showGrade,
    Boolean showQuestionResults,
    Boolean showCorrectAnswers
) {
    public UpdateAssignmentRequest(Instant dueDate, Integer maxScore, List<Long> attachments, String status) {
        this(dueDate, maxScore, attachments, status,
                null, null, null, null, null, null, null, null, null);
    }
}
