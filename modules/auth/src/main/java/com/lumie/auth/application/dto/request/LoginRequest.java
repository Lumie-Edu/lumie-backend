package com.lumie.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import com.lumie.common.validation.LoginId;
import com.lumie.common.validation.LoginIdPolicy;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import io.swagger.v3.oas.annotations.media.Schema;

@Builder
public record LoginRequest(
        @LoginId
        @Schema(minLength = LoginIdPolicy.MIN_LENGTH, maxLength = LoginIdPolicy.MAX_LENGTH,
                pattern = LoginIdPolicy.PATTERN, requiredMode = Schema.RequiredMode.REQUIRED)
        String userLoginId,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password
) {
}
