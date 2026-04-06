package com.lumie.assignment.application.dto;

public record UpdateSubmissionRequest(
    Integer score,
    String feedback,
    String status
) {
}
