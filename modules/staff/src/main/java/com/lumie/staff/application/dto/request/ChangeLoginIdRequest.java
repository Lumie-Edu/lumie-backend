package com.lumie.staff.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.lumie.common.validation.LoginId;
import com.lumie.common.validation.LoginIdPolicy;

/**
 * Request body for {@code PATCH /v1/staff/{id}/login-id} and
 * {@code PATCH /v1/students/{id}/login-id}. Replaces a former
 * {@code Map<String, String>} body with a single {@code "loginId"} key.
 */
public record ChangeLoginIdRequest(
        @LoginId
        @Schema(minLength = LoginIdPolicy.MIN_LENGTH, maxLength = LoginIdPolicy.MAX_LENGTH,
                pattern = LoginIdPolicy.PATTERN, requiredMode = Schema.RequiredMode.REQUIRED)
        String loginId
) {
}
