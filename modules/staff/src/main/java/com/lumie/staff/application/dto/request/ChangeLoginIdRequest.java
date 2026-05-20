package com.lumie.staff.application.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code PATCH /v1/staff/{id}/login-id} and
 * {@code PATCH /v1/students/{id}/login-id}. Replaces a former
 * {@code Map<String, String>} body with a single {@code "loginId"} key.
 */
public record ChangeLoginIdRequest(
        @NotBlank(message = "loginId is required")
        String loginId
) {
}
