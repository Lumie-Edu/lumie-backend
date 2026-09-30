package com.lumie.student.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.lumie.common.validation.LoginId;
import com.lumie.common.validation.LoginIdPolicy;

/**
 * Request body for {@code PATCH /v1/students/{id}/login-id}.
 */
public record ChangeLoginIdRequest(
        @LoginId
        @Schema(minLength = LoginIdPolicy.MIN_LENGTH, maxLength = LoginIdPolicy.MAX_LENGTH,
                pattern = LoginIdPolicy.PATTERN, requiredMode = Schema.RequiredMode.REQUIRED)
        String loginId
) {
}
