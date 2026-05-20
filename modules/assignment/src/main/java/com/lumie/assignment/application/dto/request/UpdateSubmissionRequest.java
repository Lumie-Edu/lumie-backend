package com.lumie.assignment.application.dto.request;

public record UpdateSubmissionRequest(
    Integer score,
    String feedback,
    String status
) {
}
