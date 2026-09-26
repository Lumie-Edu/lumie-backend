package com.lumie.auth.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record LoginRequest(
        @NotBlank(message = "User login ID is required")
        @Size(min = 4, max = 50, message = "User login ID must be between 4 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "User login ID can only contain letters, numbers, and underscores")
        String userLoginId,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password
) {
}
