package com.lumie.student.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code PATCH /v1/students/{id}/login-id}.
 */
public record ChangeLoginIdRequest(
        @NotBlank(message = "loginId is required")
        String loginId
) {
}
