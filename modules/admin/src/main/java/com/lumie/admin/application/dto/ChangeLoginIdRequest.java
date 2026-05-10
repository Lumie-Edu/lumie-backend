package com.lumie.admin.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code PATCH /v1/admins/{id}/login-id} and
 * {@code PATCH /v1/students/{id}/login-id}. Replaces a former
 * {@code Map<String, String>} body with a single {@code "loginId"} key.
 */
public record ChangeLoginIdRequest(
        @NotBlank(message = "loginId is required")
        String loginId
) {
}
