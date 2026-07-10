package com.lumie.assignment.application.dto.request;

public record UpdateSubmissionRequest(
    Integer score,
    Boolean passed,
    String gradeValue,
    String feedback,
    String status
) {
    public UpdateSubmissionRequest(Integer score, String feedback, String status) {
        this(score, null, null, feedback, status);
    }
}
