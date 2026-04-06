package com.lumie.cls.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateEnrollmentRequest(
    @NotBlank(message = "Status is required")
    String status
) {
}
